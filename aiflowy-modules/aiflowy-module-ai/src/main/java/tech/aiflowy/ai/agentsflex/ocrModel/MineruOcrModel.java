package tech.aiflowy.ai.agentsflex.ocrModel;

import com.agentsflex.core.model.ocr.OcrRequest;
import com.agentsflex.core.model.ocr.OcrResponse;

/**
 * 项目扩展的 MinerU 云端 OCR 模型。
 *
 * <p>继承官方 MineruOcrModel 复用其提交/查询/鉴权能力，仅重写 {@link #recognize(OcrRequest)}
 * 在调用官方逻辑前，把 {@link MineruOcrModelConfig} 中的项目特有参数以 snake_case key
 * 注入 {@link OcrRequest#getOptions()}，从而与 MinerU 云端接口字段保持一致。</p>
 */
public class MineruOcrModel extends com.agentsflex.ocr.mineru.MineruOcrModel {

    private final MineruOcrModelConfig config;

    public MineruOcrModel(MineruOcrModelConfig config) {
        super(config);
        this.config = config;
    }

    @Override
    public OcrResponse recognize(OcrRequest request) {
        applyExtraOptions(request);
        return super.recognize(request);
    }

    /**
     * 把项目特有参数写入请求 options（snake_case），供官方提交逻辑透传给 MinerU。
     */
    private void applyExtraOptions(OcrRequest request) {
        if (request == null || config == null) {
            return;
        }
        if (config.getEnableTable() != null) {
            request.putOption("enable_table", config.getEnableTable());
        }
        if (config.getEnableFormula() != null) {
            request.putOption("enable_formula", config.getEnableFormula());
        }
        if (config.getIsOcr() != null) {
            request.putOption("is_ocr", config.getIsOcr());
        }
        if (config.getLanguage() != null) {
            request.putOption("language", config.getLanguage());
        }
        if (config.getPageRanges() != null) {
            request.putOption("page_ranges", config.getPageRanges());
        }
        if (config.getNoCache() != null) {
            request.putOption("no_cache", config.getNoCache());
        }
        if (config.getCacheTolerance() != null) {
            request.putOption("cache_tolerance", config.getCacheTolerance());
        }
    }
}
