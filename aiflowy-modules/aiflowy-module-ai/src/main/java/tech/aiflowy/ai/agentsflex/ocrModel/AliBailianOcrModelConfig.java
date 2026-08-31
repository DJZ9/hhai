package tech.aiflowy.ai.agentsflex.ocrModel;

import com.agentsflex.core.model.ocr.BaseOcrConfig;

/**
 * 阿里百炼 OCR 模型配置。
 *
 * <p>继承 {@link BaseOcrConfig} 复用通用端点/模型/apiKey 配置，保留百炼特有的
 * OCR 任务类型、模型名称与工作空间 ID，并提供默认端点推导逻辑。</p>
 */
public class AliBailianOcrModelConfig extends BaseOcrConfig {

    /**
     * OCR 任务类型，默认 document_parsing（文档解析）
     */
    private String ocrTask = "document_parsing";

    /**
     * 模型名称，默认 qwen3.5-ocr
     */
    private String modelName = "qwen3.5-ocr";

    /**
     * Workspace ID（业务空间ID），华北2（北京）地域需要填写
     */
    private String workspaceId;

    public String getOcrTask() {
        return ocrTask;
    }

    public void setOcrTask(String ocrTask) {
        this.ocrTask = ocrTask;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public String getEndpoint() {
        String endpoint = super.getEndpoint();
        if (endpoint != null && !endpoint.isEmpty()) {
            return endpoint;
        }
        if (workspaceId != null && !workspaceId.isEmpty()) {
            return "https://" + workspaceId + ".cn-beijing.maas.aliyuncs.com/compatible-mode/v1";
        }
        return "https://dashscope.aliyuncs.com/compatible-mode/v1";
    }
}
