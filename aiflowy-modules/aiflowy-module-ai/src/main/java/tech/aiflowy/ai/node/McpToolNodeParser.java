package tech.aiflowy.ai.node;

import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.node.BaseNode;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.math.BigInteger;

public class McpToolNodeParser extends BaseNodeParser {

    @Override
    public BaseNode doParse(JSONObject root, JSONObject data, JSONObject tinyflow) {
        BigInteger mcpToolId = data.getBigInteger("mcpId");
        String mcpToolName = data.getString("mcpToolName");
        return new McpToolNode(mcpToolId, mcpToolName);
    }

    public String getNodeName() {
        return "mcp-node";
    }
}
