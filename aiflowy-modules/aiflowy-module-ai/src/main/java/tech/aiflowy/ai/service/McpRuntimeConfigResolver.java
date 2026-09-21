package tech.aiflowy.ai.service;

import org.springframework.stereotype.Component;
import tech.aiflowy.ai.dto.McpCatalogEntry;
import tech.aiflowy.ai.dto.ResolvedMcpConfig;
import tech.aiflowy.ai.entity.Mcp;
import tech.aiflowy.ai.utils.McpConfigSecurityUtil;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.exceptions.BusinessException;

/**
 * 统一解析 MCP 记录到运行时可注册配置。
 */
@Component
public class McpRuntimeConfigResolver {

    private final McpAdminCatalogRegistry catalogRegistry;

    public McpRuntimeConfigResolver(McpAdminCatalogRegistry catalogRegistry) {
        this.catalogRegistry = catalogRegistry;
    }

    public boolean isCatalogReference(Mcp mcp) {
        return mcp != null && McpConfigSecurityUtil.getCatalogKey(mcp.getConfigJson()) != null;
    }

    public String getCatalogKey(Mcp mcp) {
        return mcp == null ? null : McpConfigSecurityUtil.getCatalogKey(mcp.getConfigJson());
    }

    public ResolvedMcpConfig resolve(Mcp mcp) {
        if (mcp == null) {
            throw new BusinessException("MCP 不存在");
        }
        String catalogKey = getCatalogKey(mcp);
        if (catalogKey != null) {
            return resolveCatalog(mcp, catalogKey);
        }

        Result<?> validation = McpConfigSecurityUtil.validateRemoteOnly(mcp.getConfigJson());
        if (validation.getErrorCode() != 0) {
            throw new BusinessException(validation.getMessage());
        }
        String baseName = "aiflowy-http-" + mcp.getId();
        McpConfigSecurityUtil.ManagedConfigHolder holder = new McpConfigSecurityUtil.ManagedConfigHolder();
        String managedName = McpConfigSecurityUtil.toManagedConfigJsonAndGetName(mcp.getConfigJson(), baseName, holder);

        ResolvedMcpConfig resolved = new ResolvedMcpConfig();
        resolved.setManagedServerName(managedName);
        resolved.setRuntimeConfigJson(holder.getConfigJson());
        resolved.setShared(false);
        resolved.setProtocol("http");
        return resolved;
    }

    private ResolvedMcpConfig resolveCatalog(Mcp mcp, String catalogKey) {
        McpCatalogEntry entry = catalogRegistry.get(catalogKey);
        if (entry == null || !entry.isEnabled()) {
            throw new BusinessException("管理员 MCP 目录条目不存在或已停用: " + catalogKey);
        }
        String baseName = "aiflowy-catalog-" + catalogKey;
        McpConfigSecurityUtil.ManagedConfigHolder holder = new McpConfigSecurityUtil.ManagedConfigHolder();
        String managedName = McpConfigSecurityUtil.toManagedConfigJsonAndGetName(entry.getConfigJson(), baseName, holder);

        ResolvedMcpConfig resolved = new ResolvedMcpConfig();
        resolved.setManagedServerName(managedName);
        resolved.setRuntimeConfigJson(holder.getConfigJson());
        resolved.setShared(true);
        resolved.setCatalogKey(catalogKey);
        resolved.setProtocol(entry.getProtocol());
        return resolved;
    }
}
