package tech.aiflowy.ai.service.impl;

import com.agentsflex.core.model.chat.tool.Parameter;
import com.agentsflex.core.model.chat.tool.Tool;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.tenant.TenantManager;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.aiflowy.ai.agentsflex.tool.McpTool;
import tech.aiflowy.ai.dto.McpCatalogEntry;
import tech.aiflowy.ai.dto.McpCatalogImportDTO;
import tech.aiflowy.ai.dto.McpCatalogItemDTO;
import tech.aiflowy.ai.dto.ResolvedMcpConfig;
import tech.aiflowy.ai.entity.BotMcp;
import tech.aiflowy.ai.entity.Mcp;
import tech.aiflowy.ai.mapper.McpMapper;
import tech.aiflowy.ai.service.McpAdminCatalogRegistry;
import tech.aiflowy.ai.service.McpService;
import tech.aiflowy.ai.service.McpRuntimeConfigResolver;
import tech.aiflowy.ai.service.McpSharedRuntimeManager;
import tech.aiflowy.ai.utils.CommonFiledUtil;
import tech.aiflowy.ai.utils.McpConfigSecurityUtil;
import tech.aiflowy.common.constant.enums.EnumRes;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.util.StringUtil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

/**
 *  服务层实现。
 *
 * @author wangGangQiang
 * @since 2026-01-04
 */
@Service
public class McpServiceImpl extends ServiceImpl<McpMapper, Mcp>  implements McpService{
    private final Logger Log = LoggerFactory.getLogger(McpServiceImpl.class);

    @Resource
    private McpRuntimeConfigResolver mcpRuntimeConfigResolver;

    @Resource
    private McpSharedRuntimeManager mcpSharedRuntimeManager;

    @Resource
    private McpAdminCatalogRegistry mcpAdminCatalogRegistry;

    @Override
    public Result<?> saveMcp(Mcp entity) {
        Result<?> validateResult = validateMcpConfig(entity);
        if (!(EnumRes.SUCCESS.getCode() == validateResult.getErrorCode())) {
            return validateResult;
        }
        String serverName = getFirstMcpServerName(entity.getConfigJson());
        if (!StringUtil.hasText(serverName)) {
            return Result.fail("未找到mcp服务名称", serverName);
        }

        LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
        CommonFiledUtil.commonFiled(entity, loginAccount.getId(), loginAccount.getTenantId(), loginAccount.getDeptId());
        this.save(entity);

        try {
            ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(entity);
            if (Boolean.TRUE.equals(entity.getStatus())) {
                mcpSharedRuntimeManager.start(resolved);
            } else {
                mcpSharedRuntimeManager.register(resolved);
            }
        } catch (Exception e) {
            Log.error("MCP服务名称：{} 注册失败", serverName, e);
            this.removeById(entity.getId());
            return Result.fail("MCP服务名称：" + serverName + " 启动失败", serverName);
        }
        return Result.ok();
    }

    @Override
    public Result<?> updateMcp(Mcp entity) {
        if (entity == null || entity.getId() == null) {
            return Result.fail("MCP id 不能为空");
        }
        Mcp existing = this.getById(entity.getId());
        if (existing == null) {
            return Result.fail("MCP 不存在");
        }
        if (!StringUtil.hasText(entity.getConfigJson())) {
            entity.setConfigJson(existing.getConfigJson());
        }

        String existingCatalogKey = mcpRuntimeConfigResolver.getCatalogKey(existing);
        String requestCatalogKey = McpConfigSecurityUtil.getCatalogKey(entity.getConfigJson());
        if (existingCatalogKey != null) {
            if (!Objects.equals(existingCatalogKey, requestCatalogKey)) {
                return Result.fail("管理员目录引用不可修改，请删除后重新安装");
            }
        } else {
            Result<?> validateResult = validateMcpConfig(entity);
            if (!(EnumRes.SUCCESS.getCode() == validateResult.getErrorCode())) {
                return validateResult;
            }
        }

        entity.setModified(new Date());

        try {
            ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(entity);
            LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
            if (Boolean.TRUE.equals(entity.getStatus())) {
                mcpSharedRuntimeManager.start(resolved);
                Log.info("共享 MCP 启动，userId={}, mcpId={}, catalogKey={}",
                        loginAccount.getId(), entity.getId(), resolved.getCatalogKey());
            } else {
                entity.setClientOnline(false);
                mcpSharedRuntimeManager.stop(resolved);
                Log.info("共享 MCP 停止，userId={}, mcpId={}, catalogKey={}",
                        loginAccount.getId(), entity.getId(), resolved.getCatalogKey());
            }
        } catch (Exception e) {
            Log.error("MCP更新失败，id: {}", entity.getId(), e);
            return Result.fail("MCP服务启动失败：" + e.getMessage());
        }
        this.updateById(entity);
        return Result.ok();
    }

    @Override
    public void removeMcp(Serializable id) {
        Mcp mcp = this.getById(id);
        if (mcp != null) {
            try {
                ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(mcp);
                if (!resolved.isShared() || !hasOtherCatalogReference(resolved.getCatalogKey(), mcp.getId())) {
                    mcpSharedRuntimeManager.stop(resolved);
                }
            } catch (Exception e) {
                Log.warn("移除 MCP 时关闭客户端失败，id: {}", id, e);
            }
        }
        this.removeById(id);
    }

    @Override
    public Result<Page<Mcp>> pageMcp(Result<Page<Mcp>> page) {
        List<Mcp> records = page.getData().getRecords();
        records.forEach(mcp -> {
                    try {
                        ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(mcp);
                        mcp.setClientOnline(mcpSharedRuntimeManager.isOnline(resolved));
                    } catch (Exception e) {
                        mcp.setClientOnline(false);
                    }
                }
        );
        page.getData().setRecords(records);
        return page;
    }

    @Override
    public Mcp getMcpTools(String id) {
        Mcp mcp = this.getById(id);
        if (mcp != null && mcp.getStatus()) {
            ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(mcp);
            McpSyncClient mcpClient = mcpSharedRuntimeManager.getClient(resolved);
            List<McpSchema.Tool> tools = null;
            if (mcpClient != null) {
                tools = mcpClient.listTools().tools();
            }
            mcp.setTools(tools);
        }
        return mcp;
    }

    @Override
    public Tool toFunction(BotMcp botMcp) {
        Mcp mcpInfo = this.getById(botMcp.getMcpId());
        if (mcpInfo == null || !Boolean.TRUE.equals(mcpInfo.getStatus())) {
            return null;
        }
        try {
            ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(mcpInfo);
            McpSyncClient mcpClient = mcpSharedRuntimeManager.getClient(resolved);
            List<McpSchema.Tool> tools = mcpClient.listTools().tools();
            for (McpSchema.Tool tool : tools) {
                if (tool.name().equals(botMcp.getMcpToolName())) {
                    Map<String, Object> inputSchema = tool.inputSchema();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
                    McpTool mcpTool = new McpTool();
                    mcpTool.setName(tool.name());
                    mcpTool.setDescription(tool.description());
                    List<Parameter> paramList = new ArrayList<>();
                    Set<String> keySet = properties.keySet();
                    keySet.forEach(key -> {
                        Parameter parameter = new Parameter();
                        parameter.setName(key);
                        LinkedHashMap params = (LinkedHashMap) properties.get(key);
                        Set<Object> paramsKeySet = params.keySet();
                        paramsKeySet.forEach(paramsKey -> {
                            if (paramsKey.equals("type")) {
                                parameter.setType((String) params.get(paramsKey));
                            } else if (paramsKey.equals("description")) {
                                parameter.setDescription((String) params.get(paramsKey));
                            }
                        });
                        paramList.add(parameter);
                        Parameter[] parametersArr = paramList.toArray(new Parameter[properties.size()]);
                        mcpTool.setParameters(parametersArr);
                    });
                    mcpTool.setMcpId(mcpInfo.getId());
                    // 设置Bot自定义输出模板
                    String customTemplate = botMcp.getOutputTemplate();
                    if (customTemplate != null && !customTemplate.isEmpty()) {
                        mcpTool.setBotOutputTemplate(customTemplate);
                    }
                    return mcpTool;
                }
            }
        } catch (Exception e) {
            Log.warn("加载 MCP 工具失败，mcpId: {}", botMcp.getMcpId(), e);
        }
        return null;
    }

    @Override
    public Tool getRuntimeTool(BigInteger mcpId, String toolName) {
        Mcp mcp = this.getById(mcpId);
        if (mcp == null || !Boolean.TRUE.equals(mcp.getStatus())) {
            return null;
        }
        try {
            ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(mcp);
            McpSyncClient mcpClient = mcpSharedRuntimeManager.getClient(resolved);
            if (mcpClient == null) {
                return null;
            }
            for (McpSchema.Tool tool : mcpClient.listTools().tools()) {
                if (tool.name().equals(toolName)) {
                    com.agentsflex.mcp.client.McpTool runtimeTool = new com.agentsflex.mcp.client.McpTool(mcpClient, tool);
                    return runtimeTool;
                }
            }
        } catch (Exception e) {
            Log.warn("加载运行时 MCP 工具失败，mcpId: {}", mcpId, e);
        }
        return null;
    }

    public static Set<String> getMcpServerNames(String mcpJson) {
        JSONObject rootJson = JSON.parseObject(mcpJson);

        JSONObject mcpServersJson = rootJson.getJSONObject("mcpServers");
        if (mcpServersJson == null) {
            return Set.of();
        }

        // 提取 mcpServers 的所有键 → 即为 MCP 服务名称（如 everything）
        return mcpServersJson.keySet();
    }

    public static String getFirstMcpServerName(String mcpJson) {
        Set<String> serverNames = getMcpServerNames(mcpJson);
        Optional<String> firstServerName = serverNames.stream().findFirst();
        return firstServerName.orElse(null);
    }

    @Override
    public Page<Mcp> pageTools(Page<Mcp> page) {
        List<Mcp> filteredRecords = new ArrayList<>();
        for (Mcp mcp : page.getRecords()) {
            if (!mcp.getStatus()) {
                continue;
            }
            try {
                ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(mcp);
                boolean clientOnline = mcpSharedRuntimeManager.isOnline(resolved);
                mcp.setClientOnline(clientOnline);
                if (!clientOnline) {
                    continue;
                }
                McpSyncClient mcpClient = mcpSharedRuntimeManager.getClient(resolved);
                List<McpSchema.Tool> tools = mcpClient.listTools().tools();
                mcp.setTools(tools);
                filteredRecords.add(mcp);
            } catch (Exception e) {
                Log.warn("MCP 获取工具失败，id: {}", mcp.getId(), e);
            }
        }
        page.setRecords(filteredRecords);
        return page;
    }

    @Override
    public com.alibaba.fastjson2.JSONObject getMcpToolData(java.math.BigInteger mcpId, String toolName) {
        com.alibaba.fastjson2.JSONObject nodeData = new com.alibaba.fastjson2.JSONObject();
        Mcp mcp = this.getById(mcpId);
        if (mcp == null) {
            return nodeData;
        }
        nodeData.put("mcpId", mcp.getId().toString());
        nodeData.put("mcpName", mcp.getTitle());
        nodeData.put("mcpToolName", toolName);

        try {
            ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(mcp);
            McpSyncClient mcpClient = mcpSharedRuntimeManager.getClient(resolved);
            if (mcpClient != null) {
                java.util.List<McpSchema.Tool> tools = mcpClient.listTools().tools();
                for (McpSchema.Tool tool : tools) {
                    if (tool.name().equals(toolName)) {
                        java.util.Map<String, Object> inputSchema = tool.inputSchema();
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> properties = (java.util.Map<String, Object>) inputSchema.get("properties");
                        @SuppressWarnings("unchecked")
                        java.util.List<String> requiredFields = (java.util.List<String>) inputSchema.get("required");
                        com.alibaba.fastjson2.JSONArray parameters = new com.alibaba.fastjson2.JSONArray();
                        if (properties != null) {
                            for (String key : properties.keySet()) {
                                com.alibaba.fastjson2.JSONObject param = new com.alibaba.fastjson2.JSONObject();
                                param.put("name", key);
                                java.util.LinkedHashMap property = (java.util.LinkedHashMap) properties.get(key);
                                param.put("dataType", property.get("type"));
                                param.put("description", property.get("description"));
                                param.put("required", requiredFields != null && requiredFields.contains(key));
                                parameters.add(param);
                            }
                        }
                        nodeData.put("parameters", parameters);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.warn("MCP 工具信息加载失败，mcpId: {}", mcpId, e);
        }
        nodeData.put("outputDefs", new com.alibaba.fastjson2.JSONArray());
        return nodeData;
    }

    @Override
    public List<McpCatalogItemDTO> listCatalog() {
        List<McpCatalogItemDTO> result = new ArrayList<>();
        for (McpCatalogEntry entry : mcpAdminCatalogRegistry.listEnabled()) {
            McpCatalogItemDTO item = new McpCatalogItemDTO();
            item.setKey(entry.getKey());
            item.setTitle(entry.getTitle());
            item.setDescription(entry.getDescription());
            item.setProtocol(entry.getProtocol());
            item.setRunning(mcpSharedRuntimeManager.isSharedOnline(entry.getKey()));
            result.add(item);
        }
        return result;
    }

    @Override
    public Result<?> importFromCatalog(McpCatalogImportDTO request) {
        if (request == null || !StringUtil.hasText(request.getCatalogKey())) {
            return Result.fail("catalogKey 不能为空");
        }
        McpCatalogEntry entry = mcpAdminCatalogRegistry.get(request.getCatalogKey());
        if (entry == null || !entry.isEnabled()) {
            return Result.fail("管理员 MCP 目录条目不存在或已停用");
        }

        Mcp entity = new Mcp();
        entity.setTitle(StringUtil.hasText(request.getTitle()) ? request.getTitle() : entry.getTitle());
        entity.setDescription(StringUtil.hasText(request.getDescription()) ? request.getDescription() : entry.getDescription());
        entity.setConfigJson(McpConfigSecurityUtil.buildCatalogReference(entry.getKey()));
        entity.setStatus(false);

        LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
        CommonFiledUtil.commonFiled(entity, loginAccount.getId(), loginAccount.getTenantId(), loginAccount.getDeptId());
        this.save(entity);
        return Result.ok();
    }

    private Result<?> validateMcpConfig(Mcp entity) {
        if (entity == null || !StringUtil.hasText(entity.getConfigJson())) {
            Log.error("MCP 配置不能为空");
            return Result.fail("MCP 配置 JSON 不能为空");
        }
        return McpConfigSecurityUtil.validateRemoteOnly(entity.getConfigJson());
    }

    private boolean hasOtherCatalogReference(String catalogKey, BigInteger excludeId) {
        if (!StringUtil.hasText(catalogKey)) {
            return false;
        }
        TenantManager.ignoreTenantCondition();
        try {
            List<Mcp> all = this.list();
            for (Mcp mcp : all) {
                if (mcp == null || Objects.equals(mcp.getId(), excludeId)) {
                    continue;
                }
                if (Boolean.TRUE.equals(mcp.getStatus())
                        && Objects.equals(catalogKey, mcpRuntimeConfigResolver.getCatalogKey(mcp))) {
                    return true;
                }
            }
            return false;
        } finally {
            TenantManager.restoreTenantCondition();
        }
    }
}
