package tech.aiflowy.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.config.McpAdminCatalogProperties;
import tech.aiflowy.ai.dto.McpCatalogEntry;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 启动时加载管理员 MCP 目录。
 */
@Component
public class McpAdminCatalogRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(McpAdminCatalogRegistry.class);
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final long MAX_FILE_SIZE = 1024 * 1024L;

    private final McpAdminCatalogProperties properties;
    private volatile Map<String, McpCatalogEntry> entries = Map.of();

    public McpAdminCatalogRegistry(McpAdminCatalogProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void load() {
        Map<String, McpCatalogEntry> loaded = new LinkedHashMap<>();
        loadClasspathCatalog(loaded);
        loadDirectoryCatalog(loaded);

        entries = Map.copyOf(loaded);
        LOG.info("MCP 管理员目录加载完成，条目数: {}", entries.size());
    }

    private void loadClasspathCatalog(Map<String, McpCatalogEntry> loaded) {
        String resourcePath = properties.getAdminConfigClasspath();
        if (!hasText(resourcePath)) {
            LOG.debug("MCP 管理员 classpath 清单未配置，跳过 classpath 加载");
            return;
        }
        resourcePath = resourcePath.trim();
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        ClassPathResource resource = new ClassPathResource(resourcePath, getClass().getClassLoader());
        if (!resource.exists()) {
            LOG.debug("MCP 管理员 classpath 清单不存在: {}", resourcePath);
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length > MAX_FILE_SIZE) {
                LOG.warn("跳过 MCP 管理员 classpath 清单，文件过大: {}", resourcePath);
                return;
            }
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            JSONObject rootJson = JSON.parseObject(content);
            JSONObject entriesJson = rootJson == null ? null : rootJson.getJSONObject("entries");
            if (entriesJson == null || entriesJson.isEmpty()) {
                LOG.warn("跳过 MCP 管理员 classpath 清单，entries 为空: {}", resourcePath);
                return;
            }
            for (Map.Entry<String, Object> entry : entriesJson.entrySet()) {
                String key = entry.getKey();
                if (!KEY_PATTERN.matcher(key).matches()) {
                    LOG.warn("跳过 MCP classpath 条目，key 非法: {}，来源: {}", key, resourcePath);
                    continue;
                }
                if (!(entry.getValue() instanceof JSONObject entryJson)) {
                    LOG.warn("跳过 MCP classpath 条目，配置格式非法: {}，来源: {}", key, resourcePath);
                    continue;
                }
                McpCatalogEntry catalogEntry = parseEntry(key, entryJson, "classpath:" + resourcePath);
                if (catalogEntry != null) {
                    loaded.put(key, catalogEntry);
                }
            }
        } catch (Exception e) {
            LOG.warn("读取 MCP 管理员 classpath 清单失败: {}，原因: {}", resourcePath, e.getMessage());
        }
    }

    private void loadDirectoryCatalog(Map<String, McpCatalogEntry> loaded) {
        String configuredDir = properties.getAdminConfigDir();
        if (!hasText(configuredDir)) {
            LOG.debug("MCP 管理员文件目录未配置，跳过目录加载");
            return;
        }

        Path root;
        try {
            root = Paths.get(configuredDir.trim()).toAbsolutePath().normalize();
        } catch (Exception e) {
            LOG.warn("MCP 管理员目录路径非法: {}", configuredDir);
            return;
        }

        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            LOG.warn("MCP 管理员目录不存在或不是目录: {}", root);
            return;
        }

        try (Stream<Path> stream = Files.list(root)) {
            stream.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> loadFile(path, loaded));
        } catch (IOException e) {
            LOG.error("扫描 MCP 管理员目录失败: {}", root, e);
        }
    }

    private void loadFile(Path file, Map<String, McpCatalogEntry> loaded) {
        String fileName = file.getFileName().toString();
        String key = fileName.substring(0, fileName.length() - ".json".length());
        if (!KEY_PATTERN.matcher(key).matches()) {
            LOG.warn("跳过 MCP 管理员目录条目，文件名非法: {}", fileName);
            return;
        }

        try {
            if (Files.size(file) > MAX_FILE_SIZE) {
                LOG.warn("跳过 MCP 管理员目录条目，文件过大: {}", fileName);
                return;
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            JSONObject rootJson = JSON.parseObject(content);
            McpCatalogEntry entry = parseEntry(key, rootJson, fileName);
            if (entry == null) {
                return;
            }
            if (loaded.containsKey(key)) {
                LOG.info("MCP 管理员目录条目覆盖 classpath 条目: {}", key);
            }
            loaded.put(key, entry);
        } catch (Exception e) {
            LOG.warn("跳过 MCP 管理员目录条目，解析失败: {}，原因: {}", fileName, e.getMessage());
        }
    }

    private McpCatalogEntry parseEntry(String key, JSONObject rootJson, String sourceName) {
        if (rootJson == null) {
            LOG.warn("跳过 MCP 管理员目录条目，配置为空: {}", sourceName);
            return null;
        }
        JSONObject config = rootJson.getJSONObject("config");
        JSONObject servers = config == null ? null : config.getJSONObject("mcpServers");
        if (servers == null || servers.size() != 1) {
            LOG.warn("跳过 MCP 管理员目录条目，配置必须且只能包含一个 mcpServers 条目: {}", sourceName);
            return null;
        }

        JSONObject server = servers.values().stream()
                .findFirst()
                .filter(JSONObject.class::isInstance)
                .map(JSONObject.class::cast)
                .orElse(null);
        if (server == null) {
            LOG.warn("跳过 MCP 管理员目录条目，mcpServers 配置非法: {}", sourceName);
            return null;
        }

        McpCatalogEntry entry = new McpCatalogEntry();
        entry.setKey(key);
        entry.setTitle(defaultText(rootJson.getString("title"), key));
        entry.setDescription(defaultText(rootJson.getString("description"), ""));
        entry.setEnabled(rootJson.getBooleanValue("enabled", true));
        entry.setProtocol(resolveProtocol(server));
        entry.setConfigJson(config.toJSONString());
        return entry;
    }

    private String resolveProtocol(JSONObject server) {
        String transport = firstText(server.getString("transport"), server.getString("type"));
        if (transport == null) {
            String url = server.getString("url");
            if (url != null && !url.isBlank()) {
                return "http";
            }
            String command = server.getString("command");
            return command != null && !command.isBlank() ? "stdio" : "unknown";
        }
        return transport.trim().toLowerCase();
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public List<McpCatalogEntry> listEnabled() {
        return entries.values().stream()
                .filter(McpCatalogEntry::isEnabled)
                .sorted(Comparator.comparing(McpCatalogEntry::getKey))
                .toList();
    }

    public McpCatalogEntry get(String key) {
        return entries.get(key);
    }

    public List<McpCatalogEntry> listAll() {
        return new ArrayList<>(entries.values());
    }
}
