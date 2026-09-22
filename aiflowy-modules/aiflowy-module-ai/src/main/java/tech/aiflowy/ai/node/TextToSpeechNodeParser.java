package tech.aiflowy.ai.node;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.node.BaseNode;
import dev.tinyflow.core.parser.BaseNodeParser;


/**
 * 文本转语音节点解析器。
 * <p>
 * 从工作流 JSON 节点数据中读取可选的 voice/format/speed/volume/sampleRate，
 * 构造 {@link TextToSpeechNode}。凭证直接复用系统设置「语音」标签页配置，无需选择模型。
 * 节点名称 {@code tts-node} 需与前端 {@code nodeNames.ttsNode} 保持一致。
 */
public class TextToSpeechNodeParser extends BaseNodeParser {

    @Override
    public BaseNode doParse(JSONObject root, JSONObject data, JSONObject tinyflow) {
        TextToSpeechNode node = new TextToSpeechNode();

        String voice = data.getString("voice");
        if (StrUtil.isNotBlank(voice)) {
            node.setVoice(voice);
        }
        String format = data.getString("format");
        if (StrUtil.isNotBlank(format)) {
            node.setFormat(format);
        }
        Double speed = data.getDouble("speed");
        if (speed != null) {
            node.setSpeed(speed);
        } else {
            String speedStr = data.getString("speed");
            if (StrUtil.isNotBlank(speedStr)) {
                try {
                    node.setSpeed(Double.parseDouble(speedStr.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        Integer volume = data.getInteger("volume");
        if (volume != null) {
            node.setVolume(volume);
        } else {
            String volumeStr = data.getString("volume");
            if (StrUtil.isNotBlank(volumeStr)) {
                try {
                    node.setVolume(Integer.parseInt(volumeStr.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
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
        return "tts-node";
    }
}
