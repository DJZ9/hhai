package tech.aiflowy.datacenter.entity.vo;

import java.math.BigInteger;

/**
 * 导入记录VO，用于返回导入记录信息给前端
 *
 * @author ArkLight
 * @since 2026-06-29
 */
public class ImportRecordVo {

    /**
     * 记录ID
     */
    private BigInteger recordId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 导入状态：0-处理中，1-成功，2-失败
     */
    private Integer status;

    public BigInteger getRecordId() {
        return recordId;
    }

    public void setRecordId(BigInteger recordId) {
        this.recordId = recordId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
