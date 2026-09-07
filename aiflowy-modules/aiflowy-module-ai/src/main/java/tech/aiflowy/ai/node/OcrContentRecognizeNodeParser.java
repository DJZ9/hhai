package tech.aiflowy.ai.node;

import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.node.BaseNode;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.math.BigInteger;

public class OcrContentRecognizeNodeParser extends BaseNodeParser {
    @Override
    public BaseNode doParse(JSONObject root, JSONObject data, JSONObject tinyflow) {
        BigInteger modelId = data.getBigInteger("modelId");
        return new OcrContentRecognizeNode(modelId);
    }

    public String getNodeName() {
        return "ocr-content-extraction-node";
    }
}
