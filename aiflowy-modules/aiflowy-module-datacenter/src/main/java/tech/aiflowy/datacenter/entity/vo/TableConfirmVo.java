package tech.aiflowy.datacenter.entity.vo;

import java.math.BigInteger;
import java.util.List;

/**
 * 表确认导入VO，用于用户确认后提交导入
 */
public class TableConfirmVo {

    /**
     * 解析任务ID（可选，如果使用任务ID模式）
     */
    private BigInteger taskId;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 确认的英文表名
     */
    private String tableName;

    /**
     * 确认的表描述
     */
    private String tableDesc;

    /**
     * 分类ID
     */
    private BigInteger categoryId;

    /**
     * 确认的字段列表
     */
    private List<FieldConfirmVo> fields;

    /**
     * 文件临时存储路径
     */
    private String tempFilePath;

    public BigInteger getTaskId() {
        return taskId;
    }

    public void setTaskId(BigInteger taskId) {
        this.taskId = taskId;
    }

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

    public List<FieldConfirmVo> getFields() {
        return fields;
    }

    public void setFields(List<FieldConfirmVo> fields) {
        this.fields = fields;
    }

    public String getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }

    /**
     * 字段确认VO
     */
    public static class FieldConfirmVo {

        /**
         * 原始列名
         */
        private String originalName;

        /**
         * 确认的英文列名
         */
        private String fieldName;

        /**
         * 字段描述
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
}
