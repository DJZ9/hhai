package tech.aiflowy.datacenter.entity.vo;

/**
 * 字段预览VO，用于AI智能导入预览
 */
public class FieldPreviewVo {

    /**
     * 原始列名（中文或其他语言）
     */
    private String originalName;

    /**
     * AI生成的英文列名
     */
    private String fieldName;

    /**
     * 字段描述（原列名作为注释）
     */
    private String fieldDesc;

    /**
     * 字段类型
     */
    private Integer fieldType;

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldDesc() {
        return fieldDesc;
    }

    public void setFieldDesc(String fieldDesc) {
        this.fieldDesc = fieldDesc;
    }

    public Integer getFieldType() {
        return fieldType;
    }

    public void setFieldType(Integer fieldType) {
        this.fieldType = fieldType;
    }
}
