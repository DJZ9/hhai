package tech.aiflowy.ai.entity;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * 文本拆分参数
 */
public class DocumentCollectionSplitParams implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer pageNumber = 1;
    private Integer pageSize = 10;
    /**
     * 拆分操作 textSplit 拆分预览/  saveText 保存
     */
    private String operation;
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 文件原始名称
     */
    private String fileOriginName;
    /**
     * 知识库id
     */
    private BigInteger knowledgeId;
    /**
     * 拆分器名称
     */
    private String splitterName;
    /**
     * 分段大小
     */
    private Integer chunkSize = 512;
    /**
     * 重叠大小
     */
    private Integer overlapSize = 128;
    /**
     * 正则表达式
     */
    private String regex;
    private Integer rowsPerChunk = 5;
    /**
     * Excel 分段是否包含表头（首行作为列名并在每个分块重复出现）
     */
    private Boolean includeHeader = true;
    /**
     * markDown 层级拆分级别
     */
    private Integer mdSplitterLevel;

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileOriginName() {
        return fileOriginName;
    }

    public void setFileOriginName(String fileOriginName) {
        this.fileOriginName = fileOriginName;
    }

    public BigInteger getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(BigInteger knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public String getSplitterName() {
        return splitterName;
    }

    public void setSplitterName(String splitterName) {
        this.splitterName = splitterName;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getOverlapSize() {
        return overlapSize;
    }

    public void setOverlapSize(Integer overlapSize) {
        this.overlapSize = overlapSize;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public Integer getRowsPerChunk() {
        return rowsPerChunk;
    }

    public void setRowsPerChunk(Integer rowsPerChunk) {
        this.rowsPerChunk = rowsPerChunk;
    }

    public Boolean getIncludeHeader() {
        return includeHeader;
    }

    public void setIncludeHeader(Boolean includeHeader) {
        this.includeHeader = includeHeader;
    }

    public Integer getMdSplitterLevel() {
        return mdSplitterLevel;
    }

    public void setMdSplitterLevel(Integer mdSplitterLevel) {
        this.mdSplitterLevel = mdSplitterLevel;
    }
}
