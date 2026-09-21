package tech.aiflowy.ai.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import tech.aiflowy.common.domain.Result;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * MCP 配置安全校验与目录引用格式工具。
 */
public final class McpConfigSecurityUtil {

    public static final String CATALOG_SOURCE_KEY = "aiflowyMcpSource";
    public static final String CATALOG_TYPE = "catalog";

    private static final Set<String> REMOTE_TRANSPORTS = Set.of(
            "sse", "http-sse", "ssehttp", "http", "streamable", "http-stream", "streamablehttp");

    private McpConfigSecurityUtil() {
    }

    public static Result<?> validateRemoteOnly(String configJson) {
        JSONObject root;
        try {
            root = JSON.parseObject(configJson);
        } catch (Exception e) {
            return Result.fail("MCP 配置 JSON 格式错误");
        }
        JSONObject servers = root == null ? null : root.getJSONObject("mcpServers");
        if (servers == null || servers.isEmpty()) {
            return Result.fail("MCP 配置必须包含非空的 mcpServers");
        }
        if (root.containsKey(CATALOG_SOURCE_KEY)) {
            return Result.fail("该配置为管理员目录引用，不能通过手动 MCP 配置保存");
        }

        for (Map.Entry<String, Object> entry : servers.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof JSONObject server)) {
                return Result.fail("MCP 服务[" + entry.getKey() + "]配置格式错误");
            }
            Result<?> result = validateRemoteServer(entry.getKey(), server);
            if (result.getErrorCode() != 0) {
                return result;
            }
        }
        return Result.ok();
    }

    private static Result<?> validateRemoteServer(String serverName, JSONObject server) {
        if (server.containsKey("command") || server.containsKey("args")
                || server.containsKey("env") || server.containsKey("cwd")) {
            return Result.fail("MCP 服务[" + serverName
                    + "]包含本地命令字段，不能从前端直接安装。请联系管理员将配置放入服务器的 MCP 配置目录，"
                    + "然后通过“添加 -> 从后台目录安装”导入。");
        }

        String transport = firstText(server.getString("transport"), server.getString("type"));
        if (transport == null) {
            if (hasText(server.getString("url"))) {
                transport = "http";
            } else {
                return Result.fail("MCP 服务[" + serverName + "]缺少 url，不能使用默认 stdio 传输");
            }
        }
        transport = transport.toLowerCase();
        if (!REMOTE_TRANSPORTS.contains(transport)) {
            return Result.fail("MCP 服务[" + serverName + "]使用了不支持的传输类型: " + transport);
        }

        String url = server.getString("url");
        if (!hasText(url)) {
            return Result.fail("MCP 服务[" + serverName + "]必须配置 url");
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getHost().isBlank()) {
                return Result.fail("MCP 服务[" + serverName + "]必须使用合法的 http/https url");
            }
        } catch (Exception e) {
            return Result.fail("MCP 服务[" + serverName + "]的 url 格式非法");
        }
        return Result.ok();
    }

    public static String buildCatalogReference(String catalogKey) {
        JSONObject source = new JSONObject();
        source.put("type", CATALOG_TYPE);
        source.put("key", catalogKey);
        JSONObject root = new JSONObject();
        root.put(CATALOG_SOURCE_KEY, source);
        return root.toJSONString();
    }

    public static String getCatalogKey(String configJson) {
        try {
            JSONObject root = JSON.parseObject(configJson);
            JSONObject source = root == null ? null : root.getJSONObject(CATALOG_SOURCE_KEY);
            if (source == null || !CATALOG_TYPE.equalsIgnoreCase(source.getString("type"))) {
                return null;
            }
            String key = source.getString("key");
            return hasText(key) ? key.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将配置中的服务名替换为应用内唯一名称，避免与 agents-flex 自带配置或其它记录冲突。
     * 返回第一个服务对应的运行时名称。
     */
    public static String toManagedConfigJsonAndGetName(String configJson, String baseName, ManagedConfigHolder holder) {
        JSONObject root = JSON.parseObject(configJson);
        JSONObject servers = root == null ? null : root.getJSONObject("mcpServers");
        if (servers == null || servers.isEmpty()) {
            throw new IllegalArgumentException("MCP 服务配置为空");
        }
        JSONObject managedServers = new JSONObject();
        int index = 0;
        String firstManagedName = null;
        for (Map.Entry<String, Object> entry : servers.entrySet()) {
            String managedName = index == 0 ? baseName : baseName + "-" + index;
            if (index == 0) {
                firstManagedName = managedName;
            }
            managedServers.put(managedName, entry.getValue());
            index++;
        }
        root.put("mcpServers", managedServers);
        holder.configJson = root.toJSONString();
        return firstManagedName;
    }

    public static String toManagedConfigJson(String configJson, String baseName) {
        ManagedConfigHolder holder = new ManagedConfigHolder();
        toManagedConfigJsonAndGetName(configJson, baseName, holder);
        return holder.configJson;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstText(String first, String second) {
        if (hasText(first)) {
            return first.trim();
        }
        if (hasText(second)) {
            return second.trim();
        }
        return null;
    }

    public static class ManagedConfigHolder {
        private String configJson;

        public String getConfigJson() {
            return configJson;
        }
    }
}
