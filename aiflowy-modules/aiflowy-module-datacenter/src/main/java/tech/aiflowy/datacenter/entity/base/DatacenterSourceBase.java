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


public class DatacenterSourceBase extends DateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId", comment = "主键")
    private BigInteger id;

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
     * 数据源类型[EnumDatasourceType]
     */
    @Column(comment = "数据源类型[EnumDatasourceType]")
    private Integer sourceType;

    /**
     * 数据源别名
     */
    @Column(comment = "数据源别名")
    private String title;

    /**
     * 数据源描述
     */
    @Column(comment = "数据源描述")
    private String description;

    /**
     * 数据库名
     */
    @Column(comment = "数据库名")
    private String sourceName;

    /**
     * 数据库地址
     */
    @Column(comment = "数据库地址")
    private String sourceAddress;

    /**
     * 用户名
     */
    @Column(comment = "用户名")
    private String username;

    /**
     * 密码
     */
    @Column(comment = "密码")
    private String password;

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

    /**
     * 权限[0全部可看，1指定部门可看]
     */
    @Column(comment = "权限[0全部可看，1指定部门可看]")
    private Integer authType;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
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

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Integer getAuthType() {
        return authType;
    }

    public void setAuthType(Integer authType) {
        this.authType = authType;
    }

}
