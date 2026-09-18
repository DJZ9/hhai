package tech.aiflowy.ai.node;

import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.node.BaseNode;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.math.BigInteger;

/**
 * 图片生成节点解析器。
 */
public class ImageGenerationNodeParser extends BaseNodeParser {

    @Override
    public BaseNode doParse(JSONObject root, JSONObject data, JSONObject tinyflow) {
        BigInteger modelId = data.getBigInteger("modelId");
        return new ImageGenerationNode(modelId);
    }

    public String getNodeName() {
        return "image-generation-node";
    }
}
