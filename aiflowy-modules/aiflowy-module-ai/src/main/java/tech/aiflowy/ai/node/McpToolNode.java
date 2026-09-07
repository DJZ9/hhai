package tech.aiflowy.ai.node;

import com.agentsflex.core.model.chat.tool.Tool;
import com.agentsflex.mcp.client.McpClientManager;
import com.alibaba.fastjson.JSON;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.BaseNode;
import tech.aiflowy.ai.entity.Mcp;
import tech.aiflowy.ai.service.McpService;
import tech.aiflowy.ai.service.impl.McpServiceImpl;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.util.StringUtil;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class McpToolNode extends BaseNode {

    private BigInteger mcpId;
    private String mcpToolName;

    public McpToolNode() {
    }

    public McpToolNode(BigInteger mcpId) {
        this.mcpId = mcpId;
    }

    public McpToolNode(BigInteger mcpId, String mcpToolName) {
        this.mcpId = mcpId;
        this.mcpToolName = mcpToolName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> execute(Chain chain) {
        Map<String, Object> map = chain.getState().resolveParameters(this);
        map.values().removeIf(Objects::isNull);
        McpService bean = SpringContextUtil.getBean(McpService.class);
        Mcp mcp = bean.getById(mcpId);
        if (mcp == null) {
            return Collections.emptyMap();
        }
        String serverName = McpServiceImpl.getFirstMcpServerName(mcp.getConfigJson());
        if (!StringUtil.hasText(serverName)) {
            return Collections.emptyMap();
        }
        Tool tool = McpClientManager.getInstance().getMcpTool(serverName, mcpToolName);
        if (tool == null) {
            return Collections.emptyMap();
        }

        Object result = tool.invoke(map);
        if (result == null) {
            return Collections.emptyMap();
        }

        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }

        if (result instanceof String) {
            return Collections.singletonMap("result", result);
        }

        return JSON.parseObject(JSON.toJSONString(result), Map.class);
    }

    public BigInteger getMcpId() {
        return mcpId;
    }

    public void setMcpId(BigInteger mcpId) {
        this.mcpId = mcpId;
    }

    public String getMcpToolName() {
        return mcpToolName;
    }

    public void setMcpToolName(String mcpToolName) {
        this.mcpToolName = mcpToolName;
    }
}
