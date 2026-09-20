package tech.aiflowy.ai.node;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.node.BaseNode;
import dev.tinyflow.core.parser.BaseNodeParser;

import java.math.BigInteger;

/**
 * 视频生成节点解析器。
 * <p>
 * 从工作流 JSON 节点数据中读取 modelId，构造 {@link VideoGenerationNode}。
 * 节点名称 {@code video-generation} 需与前端 {@code nodeNames.videoGenerationNode} 保持一致。
 */
public class VideoGenerationNodeParser extends BaseNodeParser {

    @Override
    public BaseNode doParse(JSONObject root, JSONObject data, JSONObject tinyflow) {
        BigInteger modelId = data.getBigInteger("modelId");
        if (modelId == null) {
            // 兼容字符串形态
            String raw = data.getString("modelId");
            if (StrUtil.isNotBlank(raw)) {
                try {
                    modelId = new BigInteger(raw);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (modelId == null) {
            throw new RuntimeException("请选择视频模型");
        }

        VideoGenerationNode node = new VideoGenerationNode(modelId);

        // 读取前端 forms 中配置的视频参数（分辨率、宽高比、时长、生成模式）
        String resolution = data.getString("resolution");
        if (StrUtil.isNotBlank(resolution)) {
            node.setResolution(resolution);
        }
        String aspectRatio = data.getString("aspectRatio");
        if (StrUtil.isNotBlank(aspectRatio)) {
            node.setAspectRatio(aspectRatio);
        }
        Integer duration = data.getInteger("duration");
        if (duration != null) {
            node.setDuration(duration);
        } else {
            // 兼容字符串形态
            String durationStr = data.getString("duration");
            if (StrUtil.isNotBlank(durationStr)) {
                try {
                    node.setDuration(Integer.parseInt(durationStr.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String generationMode = data.getString("generationMode");
        if (StrUtil.isNotBlank(generationMode)) {
            node.setGenerationMode(generationMode);
        }

        return node;
    }

    public String getNodeName() {
        return "video-generation";
    }
}
