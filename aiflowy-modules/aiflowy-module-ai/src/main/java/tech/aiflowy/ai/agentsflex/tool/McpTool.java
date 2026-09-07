package tech.aiflowy.ai.agentsflex.tool;

import com.agentsflex.core.model.chat.tool.BaseTool;
import com.agentsflex.core.model.chat.tool.Tool;
import com.agentsflex.core.prompt.PromptTemplate;
import com.agentsflex.mcp.client.McpClientManager;
import tech.aiflowy.ai.entity.Mcp;
import tech.aiflowy.ai.service.McpService;
import tech.aiflowy.ai.service.impl.McpServiceImpl;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.util.StringUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class McpTool extends BaseTool {
    private BigInteger mcpId;
    // mcp 自定义输出模板
    private String botOutputTemplate;

    @Override
    public Object invoke(Map<String, Object> argsMap) {
        return runMcp(this.mcpId, argsMap);
    }

    public Object runMcp(BigInteger mcpId, Map<String, Object> argsMap) {

        McpService mcpService = SpringContextUtil.getBean(McpService.class);
        Mcp mcp = mcpService.getMapper().selectOneById(mcpId);
        String serverName = McpServiceImpl.getFirstMcpServerName(mcp.getConfigJson());
        if (StringUtil.hasText(serverName)) {
            McpClientManager mcpClientManager = McpClientManager.getInstance();
            Tool mcpTool = mcpClientManager.getMcpTool(serverName, this.name);
            Object result = mcpTool.invoke(argsMap);
            // 应用Bot自定义输出模板
            if (botOutputTemplate != null && !botOutputTemplate.isEmpty()) {
                PromptTemplate promptTemplate = PromptTemplate.of(botOutputTemplate);
                promptTemplate.setKeepExpressionOnMissingVariable(true);
                if (result instanceof Map) {
                    return promptTemplate.format((Map<String, Object>) result);
                }
                // 处理其他类型结果
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("result", result);
                return promptTemplate.format(resultMap);
            }
            return Objects.requireNonNullElse(result, "");
        }
        return "未查询到相关数据";
    }

    public void setMcpId(BigInteger mcpId) {
        this.mcpId = mcpId;
    }

    public BigInteger getMcpId() {
        return mcpId;
    }

    public String getBotOutputTemplate() {
        return botOutputTemplate;
    }

    public void setBotOutputTemplate(String botOutputTemplate) {
        this.botOutputTemplate = botOutputTemplate;
    }
}
