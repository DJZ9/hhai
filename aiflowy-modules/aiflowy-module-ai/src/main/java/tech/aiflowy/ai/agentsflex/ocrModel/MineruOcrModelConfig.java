package tech.aiflowy.ai.agentsflex.ocrModel;

import com.agentsflex.ocr.mineru.MineruOcrConfig;

/**
 * 项目扩展的 MinerU OCR 配置。
 *
 * <p>继承官方 {@link MineruOcrConfig} 以复用云端 MinerU 的端点/路径/鉴权能力，
 * 同时保留项目特有的表格、公式、OCR、语言、页码范围、缓存等高级参数。</p>
 */
public class MineruOcrModelConfig extends MineruOcrConfig {

    /**
     * 是否启用表格识别，默认 true
     * 仅对 pipeline 和 vlm 模型有效
     */
    private Boolean enableTable = true;

    /**
     * 是否启用公式识别，默认 true
     * 仅对 pipeline 和 vlm 模型有效
     */
    private Boolean enableFormula = true;

    /**
     * 是否启用 OCR，默认 false
     * 仅对 pipeline 和 vlm 模型有效
     */
    private Boolean isOcr = false;

    /**
     * 文档语言，默认 ch（中文）
     */
    private String language = "ch";

    /**
     * 页面范围，格式如 "2,4-6"
     */
    private String pageRanges;

    /**
     * 是否绕过缓存，默认 false
     */
    private Boolean noCache = false;

    /**
     * 缓存容忍时间（秒），默认 900
     */
    private Integer cacheTolerance = 900;

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
