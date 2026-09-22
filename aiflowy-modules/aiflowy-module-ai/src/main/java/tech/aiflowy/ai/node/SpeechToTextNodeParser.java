package tech.aiflowy.ai.node;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.node.BaseNode;
import dev.tinyflow.core.parser.BaseNodeParser;

/**
 * 语音转文本节点解析器。
 * <p>
 * 从工作流 JSON 节点数据中读取可选的 format/sampleRate，构造 {@link SpeechToTextNode}。
 * 凭证直接复用系统设置「语音」标签页配置，无需选择模型。
 * 节点名称 {@code stt-node} 需与前端 {@code nodeNames.sttNode} 保持一致。
 */
public class SpeechToTextNodeParser extends BaseNodeParser {

    @Override
    public BaseNode doParse(JSONObject root, JSONObject data, JSONObject tinyflow) {
        SpeechToTextNode node = new SpeechToTextNode();

        String format = data.getString("format");
        if (StrUtil.isNotBlank(format)) {
            node.setFormat(format);
        }
        Integer sampleRate = data.getInteger("sampleRate");
        if (sampleRate != null) {
            node.setSampleRate(sampleRate);
        } else {
            String sampleRateStr = data.getString("sampleRate");
            if (StrUtil.isNotBlank(sampleRateStr)) {
                try {
                    node.setSampleRate(Integer.parseInt(sampleRateStr.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return node;
    }

    public String getNodeName() {
        return "stt-node";
    }
}
