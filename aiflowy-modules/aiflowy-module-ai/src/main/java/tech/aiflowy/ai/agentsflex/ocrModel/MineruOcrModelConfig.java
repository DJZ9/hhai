package tech.aiflowy.ai.agentsflex.ocrModel;

/**
 * MinerU OCR 模型配置类
 * 用于配置 MinerU 文档解析服务的参数
 */
public class MineruOcrModelConfig extends OcrModelConfig {

    /**
     * 模型版本：pipeline（默认）/ vlm（推荐）/ MinerU-HTML
     * vlm 版本支持更精准的表格和公式识别
     */
    private String modelVersion = "vlm";

    /**
     * 是否启用表格识别，默认 true
     * 仅对 pipeline 和 vlm 模型有效
     */
    private Boolean enableTable = true;

    /**
     * 是否启用公式识别，默认 true
     * 仅对 pipeline 和 vlm 模型有效
     * 注意：vlm 模型此参数只影响内联公式提取
     */
    private Boolean enableFormula = true;

    /**
     * 是否启用 OCR，默认 false
     * 仅对 pipeline 和 vlm 模型有效
     */
    private Boolean isOcr = false;

    /**
     * 文档语言，默认 ch（中文）
     * 支持：ch（中文）、en（英文）等
     * 仅对 pipeline 和 vlm 模型有效
     */
    private String language = "ch";

    /**
     * 页面范围，格式如 "2,4-6" 表示第2页和第4到6页
     * 为空表示解析全部页面
     */
    private String pageRanges;

    /**
     * 是否绕过缓存，默认 false
     */
    private Boolean noCache = false;

    /**
     * 缓存容忍时间（秒），默认 900（15分钟）
     */
    private Integer cacheTolerance = 900;

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Boolean getEnableTable() {
        return enableTable;
    }

    public void setEnableTable(Boolean enableTable) {
        this.enableTable = enableTable;
    }

    public Boolean getEnableFormula() {
        return enableFormula;
    }

    public void setEnableFormula(Boolean enableFormula) {
        this.enableFormula = enableFormula;
    }

    public Boolean getIsOcr() {
        return isOcr;
    }

    public void setIsOcr(Boolean isOcr) {
        this.isOcr = isOcr;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPageRanges() {
        return pageRanges;
    }

    public void setPageRanges(String pageRanges) {
        this.pageRanges = pageRanges;
    }

    public Boolean getNoCache() {
        return noCache;
    }

    public void setNoCache(Boolean noCache) {
        this.noCache = noCache;
    }

    public Integer getCacheTolerance() {
        return cacheTolerance;
    }

    public void setCacheTolerance(Integer cacheTolerance) {
        this.cacheTolerance = cacheTolerance;
    }
}