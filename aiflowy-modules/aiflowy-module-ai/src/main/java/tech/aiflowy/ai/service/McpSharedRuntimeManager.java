package tech.aiflowy.ai.service;

import com.agentsflex.mcp.client.McpClientManager;
import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.config.McpClientManagerDefaults;
import tech.aiflowy.ai.dto.ResolvedMcpConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 管理 MCP 运行时注册、启动和停止。目录 MCP 使用 catalogKey 共享同一实例。
 */
@Component
public class McpSharedRuntimeManager {

    private static final Logger LOG = LoggerFactory.getLogger(McpSharedRuntimeManager.class);

    private final McpClientManager clientManager;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public McpSharedRuntimeManager() {
        McpClientManagerDefaults.disableBuiltInResource();
        this.clientManager = McpClientManager.getInstance();
    }

    public void register(ResolvedMcpConfig config) {
        clientManager.registerFromJson(config.getRuntimeConfigJson());
    }

    public void start(ResolvedMcpConfig config) {
        ReentrantLock lock = lock(config);
        lock.lock();
        try {
            clientManager.registerFromJson(config.getRuntimeConfigJson());
            clientManager.getMcpClient(config.getManagedServerName());
        } catch (Exception e) {
            throw new IllegalStateException("MCP 启动失败: " + config.getManagedServerName(), e);
        } finally {
            lock.unlock();
        }
    }

    public McpSyncClient getClient(ResolvedMcpConfig config) {
        ReentrantLock lock = lock(config);
        lock.lock();
        try {
            if (!clientManager.isClientOnline(config.getManagedServerName())) {
                clientManager.registerFromJson(config.getRuntimeConfigJson());
                return clientManager.getMcpClient(config.getManagedServerName());
            }
            return clientManager.getMcpClient(config.getManagedServerName());
        } finally {
            lock.unlock();
        }
    }

    public void stop(ResolvedMcpConfig config) {
        ReentrantLock lock = lock(config);
        lock.lock();
        try {
            if (clientManager.isClientOnline(config.getManagedServerName())) {
                McpSyncClient client = clientManager.getMcpClient(config.getManagedServerName());
                if (client != null) {
                    client.close();
                }
            }
        } catch (Exception e) {
            LOG.warn("停止 MCP 失败: {}", config.getManagedServerName(), e);
        } finally {
            lock.unlock();
        }
    }

    public boolean isOnline(ResolvedMcpConfig config) {
        return clientManager.isClientOnline(config.getManagedServerName());
    }

    public boolean isSharedOnline(String catalogKey) {
        return clientManager.isClientOnline("aiflowy-catalog-" + catalogKey);
    }

    private ReentrantLock lock(ResolvedMcpConfig config) {
        String key = config.isShared() ? "catalog:" + config.getCatalogKey() : "http:" + config.getManagedServerName();
        return locks.computeIfAbsent(key, ignored -> new ReentrantLock());
    }
}
