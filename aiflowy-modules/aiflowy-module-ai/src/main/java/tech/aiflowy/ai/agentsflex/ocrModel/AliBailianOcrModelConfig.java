package tech.aiflowy.ai.agentsflex.ocrModel;

/**
 * 阿里百炼 OCR 模型配置类
 * 用于配置阿里百炼 qwen3.5-ocr 模型的参数
 */
public class AliBailianOcrModelConfig extends OcrModelConfig {

    /**
     * OCR 任务类型，默认为 document_parsing（文档解析）
     * 可选值：
     * - document_parsing: 文档解析
     * - text_extraction: 文字提取
     * - table_recognition: 表格识别
     * - formula_recognition: 公式识别
     */
    private String ocrTask = "document_parsing";

    /**
     * 模型名称，默认 qwen3.5-ocr
     */
    private String modelName = "qwen3.5-ocr";

    /**
     * Workspace ID（业务空间ID）
     * 华北2（北京）地域需要填写
     */
    private String workspaceId;

    /**
     * API 端点
     * 格式：https://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/compatible-mode/v1
     */
    private String endpoint;

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
        // 如果配置了 endpoint，优先使用
        if (endpoint != null && !endpoint.isEmpty()) {
            return endpoint;
        }
        // 如果设置了 workspaceId，使用业务空间专属域名
        if (workspaceId != null && !workspaceId.isEmpty()) {
            return "https://" + workspaceId + ".cn-beijing.maas.aliyuncs.com/compatible-mode/v1";
        }
        // 默认使用公网域名
        return "https://dashscope.aliyuncs.com/compatible-mode/v1";
    }

    @Override
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}