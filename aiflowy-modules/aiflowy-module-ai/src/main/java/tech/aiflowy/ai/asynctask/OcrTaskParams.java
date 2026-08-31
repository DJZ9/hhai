package tech.aiflowy.ai.asynctask;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;

/**
 * OCR 异步任务的提交参数：携带 modelId 与 fileUrl，由 Worker 在提交时据此动态解析 OcrModel。
 */
public class OcrTaskParams implements Serializable {
    private static final long serialVersionUID = 1L;

    /** OCR 模型 ID（tb_model.id） */
    private BigInteger modelId;
    /** 供应商可访问的文件 URL */
    private String fileUrl;
    /** 可选的 per-request 选项（snake_case key），可为空 */
    private Map<String, Object> options;

    public OcrTaskParams() {
    }

    public OcrTaskParams(BigInteger modelId, String fileUrl, Map<String, Object> options) {
        this.modelId = modelId;
        this.fileUrl = fileUrl;
        this.options = options;
    }

    public BigInteger getModelId() {
        return modelId;
    }

    public void setModelId(BigInteger modelId) {
        this.modelId = modelId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}
