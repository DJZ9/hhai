package tech.aiflowy.datacenter.entity.vo;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * 表预览VO，用于AI智能导入预览
 */
public class TablePreviewVo {

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * AI生成的英文表名（最大50字符）
     */
    private String tableName;

    /**
     * AI生成的表描述
     */
    private String tableDesc;

    /**
     * 分类ID
     */
    private BigInteger categoryId;

    /**
     * 字段预览列表
     */
    private List<FieldPreviewVo> fields;

    /**
     * 总行数
     */
    private Integer totalRows;

    /**
     * 示例数据（前5行）
     */
    private List<Map<String, Object>> sampleData;

    /**
     * 文件临时存储路径（用于后续导入）
     */
    private String tempFilePath;

    /**
     * 错误信息（解析失败时）
     */
    private String errorInfo;

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableDesc() {
        return tableDesc;
    }

    public void setTableDesc(String tableDesc) {
        this.tableDesc = tableDesc;
    }

    public BigInteger getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(BigInteger categoryId) {
        this.categoryId = categoryId;
    }

    public List<FieldPreviewVo> getFields() {
        return fields;
    }

    public void setFields(List<FieldPreviewVo> fields) {
        this.fields = fields;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public List<Map<String, Object>> getSampleData() {
        return sampleData;
    }

    public void setSampleData(List<Map<String, Object>> sampleData) {
        this.sampleData = sampleData;
    }

    public String getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }
}
