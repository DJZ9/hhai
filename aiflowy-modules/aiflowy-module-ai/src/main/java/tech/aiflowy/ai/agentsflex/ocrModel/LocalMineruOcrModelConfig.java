package tech.aiflowy.ai.agentsflex.ocrModel;

import com.agentsflex.core.model.ocr.BaseOcrConfig;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * 本地 MinerU OCR 模型配置类
 * 参数名与 MinerU FastAPI /file_parse 接口保持一致（snake_case）
 */
public class LocalMineruOcrModelConfig extends BaseOcrConfig {

    /**
     * 是否要求接口直接返回 Markdown 内容，默认 true
     */
    @JSONField(name = "return_md")
    private Boolean returnMd = true;

    /**
     * OCR 语言列表，如 ["ch"]
     */
    @JSONField(name = "lang_list")
    private List<String> langList;

    /**
     * 解析后端：pipeline / vlm-engine / vlm-http-client / hybrid-engine / hybrid-http-client
     */
    @JSONField(name = "backend")
    private String backend = "pipeline";

    /**
     * hybrid 后端 effort：medium / high
     */
    @JSONField(name = "effort")
    private String effort;

    /**
     * PDF 解析方法：auto / txt / ocr
     */
    @JSONField(name = "parse_method")
    private String parseMethod;

    /**
     * 是否启用公式解析
     */
    @JSONField(name = "formula_enable")
    private Boolean formulaEnable;

    /**
     * 是否启用表格解析
     */
    @JSONField(name = "table_enable")
    private Boolean tableEnable;

    /**
     * 是否启用图片/图表分析
     */
    @JSONField(name = "image_analysis")
    private Boolean imageAnalysis;

    /**
     * vlm/hybrid-http-client 后端使用的 OpenAI 兼容服务地址
     */
    @JSONField(name = "server_url")
    private String serverUrl;

    /**
     * 是否返回 middle JSON
     */
    @JSONField(name = "return_middle_json")
    private Boolean returnMiddleJson;

    /**
     * 是否返回 model output JSON
     */
    @JSONField(name = "return_model_output")
    private Boolean returnModelOutput;

    /**
     * 是否返回 content list JSON
     */
    @JSONField(name = "return_content_list")
    private Boolean returnContentList;

    /**
     * 是否返回提取的图片
     */
    @JSONField(name = "return_images")
    private Boolean returnImages = true;

    /**
     * 是否以 ZIP 文件格式返回结果
     */
    @JSONField(name = "response_format_zip")
    private Boolean responseFormatZip = true;

    /**
     * ZIP 结果中是否包含原始文件
     */
    @JSONField(name = "return_original_file")
    private Boolean returnOriginalFile;

    /**
     * 是否将最终 markdown/content-list 生成延迟到客户端
     */
    @JSONField(name = "client_side_output_generation")
    private Boolean clientSideOutputGeneration;

    /**
     * PDF 起始页码（从 0 开始）
     */
    @JSONField(name = "start_page_id")
    private Integer startPageId;

    /**
     * PDF 结束页码
     */
    @JSONField(name = "end_page_id")
    private Integer endPageId;

    public Boolean getReturnMd() {
        return returnMd;
    }

    public void setReturnMd(Boolean returnMd) {
        this.returnMd = returnMd;
    }

    public List<String> getLangList() {
        return langList;
    }

    public void setLangList(List<String> langList) {
        this.langList = langList;
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public String getEffort() {
        return effort;
    }

    public void setEffort(String effort) {
        this.effort = effort;
    }

    public String getParseMethod() {
        return parseMethod;
    }

    public void setParseMethod(String parseMethod) {
        this.parseMethod = parseMethod;
    }

    public Boolean getFormulaEnable() {
        return formulaEnable;
    }

    public void setFormulaEnable(Boolean formulaEnable) {
        this.formulaEnable = formulaEnable;
    }

    public Boolean getTableEnable() {
        return tableEnable;
    }

    public void setTableEnable(Boolean tableEnable) {
        this.tableEnable = tableEnable;
    }

    public Boolean getImageAnalysis() {
        return imageAnalysis;
    }

    public void setImageAnalysis(Boolean imageAnalysis) {
        this.imageAnalysis = imageAnalysis;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public Boolean getReturnMiddleJson() {
        return returnMiddleJson;
    }

    public void setReturnMiddleJson(Boolean returnMiddleJson) {
        this.returnMiddleJson = returnMiddleJson;
    }

    public Boolean getReturnModelOutput() {
        return returnModelOutput;
    }

    public void setReturnModelOutput(Boolean returnModelOutput) {
        this.returnModelOutput = returnModelOutput;
    }

    public Boolean getReturnContentList() {
        return returnContentList;
    }

    public void setReturnContentList(Boolean returnContentList) {
        this.returnContentList = returnContentList;
    }

    public Boolean getReturnImages() {
        return returnImages;
    }

    public void setReturnImages(Boolean returnImages) {
        this.returnImages = returnImages;
    }

    public Boolean getResponseFormatZip() {
        return responseFormatZip;
    }

    public void setResponseFormatZip(Boolean responseFormatZip) {
        this.responseFormatZip = responseFormatZip;
    }

    public Boolean getReturnOriginalFile() {
        return returnOriginalFile;
    }

    public void setReturnOriginalFile(Boolean returnOriginalFile) {
        this.returnOriginalFile = returnOriginalFile;
    }

    public Boolean getClientSideOutputGeneration() {
        return clientSideOutputGeneration;
    }

    public void setClientSideOutputGeneration(Boolean clientSideOutputGeneration) {
        this.clientSideOutputGeneration = clientSideOutputGeneration;
    }

    public Integer getStartPageId() {
        return startPageId;
    }

    public void setStartPageId(Integer startPageId) {
        this.startPageId = startPageId;
    }

    public Integer getEndPageId() {
        return endPageId;
    }

    public void setEndPageId(Integer endPageId) {
        this.endPageId = endPageId;
    }
}
