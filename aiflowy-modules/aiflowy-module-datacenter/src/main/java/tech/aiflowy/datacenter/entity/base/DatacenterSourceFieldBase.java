package tech.aiflowy.datacenter.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.handler.FastjsonTypeHandler;
import tech.aiflowy.common.entity.DateEntity;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;


public class DatacenterSourceFieldBase extends DateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId", comment = "主键")
    private BigInteger id;

    /**
     * 表ID
     */
    @Column(comment = "表ID")
    private BigInteger tableId;

    /**
     * 类型
     */
    @Column(comment = "类型")
    private String type;

    /**
     * 长度
     */
    @Column(comment = "长度")
    private Integer length;

    /**
     * 可为空
     */
    @Column(comment = "可为空")
    private Integer nullable;

    /**
     * 是否为主键
     */
    @Column(comment = "是否为主键")
    private Boolean primaryKey;

    /**
     * 是否自增
     */
    @Column(comment = "是否自增")
    private Boolean autoIncrement;

    /**
     * 字段名
     */
    @Column(comment = "字段名")
    private String name;

    /**
     * 描述
     */
    @Column(comment = "描述")
    private String description;

    /**
     * 部门ID
     */
    @Column(comment = "部门ID")
    private BigInteger deptId;

    /**
     * 租户ID
     */
    @Column(tenantId = true, comment = "租户ID")
    private BigInteger tenantId;

    /**
     * 数据状态
     */
    @Column(comment = "数据状态")
    private Integer status;

    /**
     * 创建时间
     */
    @Column(comment = "创建时间")
    private Date created;

    /**
     * 创建者
     */
    @Column(comment = "创建者")
    private BigInteger createdBy;

    /**
     * 修改时间
     */
    @Column(comment = "修改时间")
    private Date modified;

    /**
     * 修改者
     */
    @Column(comment = "修改者")
    private BigInteger modifiedBy;

    /**
     * 扩展项
     */
    @Column(typeHandler = FastjsonTypeHandler.class, comment = "扩展项")
    private Map<String, Object> options;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public BigInteger getTableId() {
        return tableId;
    }

    public void setTableId(BigInteger tableId) {
        this.tableId = tableId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getNullable() {
        return nullable;
    }

    public void setNullable(Integer nullable) {
        this.nullable = nullable;
    }

    public Boolean getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(Boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Boolean getAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(Boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigInteger getDeptId() {
        return deptId;
    }

    public void setDeptId(BigInteger deptId) {
        this.deptId = deptId;
    }

    public BigInteger getTenantId() {
        return tenantId;
    }

    public void setTenantId(BigInteger tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public BigInteger getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(BigInteger createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public BigInteger getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(BigInteger modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

}
