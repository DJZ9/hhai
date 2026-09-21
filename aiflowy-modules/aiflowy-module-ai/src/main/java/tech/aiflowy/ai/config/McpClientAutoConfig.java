package tech.aiflowy.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import tech.aiflowy.ai.dto.ResolvedMcpConfig;
import tech.aiflowy.ai.entity.Mcp;
import tech.aiflowy.ai.service.McpRuntimeConfigResolver;
import tech.aiflowy.ai.service.McpSharedRuntimeManager;
import tech.aiflowy.ai.service.impl.McpServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.List;

@Configuration
@DependsOn("mcpServiceImpl") // 确保 mcpService 先初始化
public class McpClientAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(McpClientAutoConfig.class);

    @Resource
    private McpServiceImpl mcpService;

    @Resource
    private McpRuntimeConfigResolver mcpRuntimeConfigResolver;

    @Resource
    private McpSharedRuntimeManager mcpSharedRuntimeManager;

    @PostConstruct
    public void initMcpClient() {
        log.info("开始初始化 MCP 客户端...");
        List<Mcp> mcpList = mcpService.list();
        log.info("获取到 MCP 配置列表，数量：{}", mcpList.size());
        mcpList.forEach(mcp -> {
            if (!mcp.getStatus()) {
                return;
            }
            try {
                ResolvedMcpConfig resolved = mcpRuntimeConfigResolver.resolve(mcp);
                if (!resolved.isShared() || !mcpSharedRuntimeManager.isOnline(resolved)) {
                    mcpSharedRuntimeManager.register(resolved);
                    if (resolved.isShared()) {
                        mcpSharedRuntimeManager.start(resolved);
                    }
                }
                log.info("MCP服务名称：{} 注册成功", mcp.getTitle());
            } catch (Exception e) {
                log.error("MCP服务名称：{} 注册失败", mcp.getTitle(), e);
            }
        });

    }

}
