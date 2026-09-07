package tech.aiflowy.ai.node;

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.BaseNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.agentsflex.core.model.ocr.OcrResponse;
import tech.aiflowy.ai.asynctask.OcrTaskService;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.common.util.SpringContextUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class OcrContentRecognizeNode extends BaseNode {

    private static final Logger log = LoggerFactory.getLogger(OcrContentRecognizeNode.class);

    private BigInteger modelId;
    @Override
    public Map<String, Object> execute(Chain chain) {
        Map<String, Object> res = new HashMap<>();

        // 获取输入参数
        Map<String, Object> map = chain.getState().resolveParameters(this);
        String fileUrl = (String) map.get("fileUrl");
        ModelService modelService = SpringContextUtil.getBean(ModelService.class);
        Model modelInstance = modelService.getModelInstance(modelId);
        if (modelInstance == null) {
            log.error("模型不存在");
            return res;
        }
        OcrTaskService ocrTaskService = SpringContextUtil.getBean(OcrTaskService.class);
        String taskId = ocrTaskService.submit(modelId, fileUrl, null, 600_000L);
        OcrResponse ocrResponse = ocrTaskService.awaitResult(taskId, 600_000L, 3_000L);
        String content = (ocrResponse.getMarkdown() != null && !ocrResponse.getMarkdown().isEmpty())
                ? ocrResponse.getMarkdown() : ocrResponse.getText();
        res.put("content", content);
        return res;
    }

    public OcrContentRecognizeNode(BigInteger modelId) {
        this.modelId = modelId;
    }

    public BigInteger getModelId() {
        return modelId;
    }

}
