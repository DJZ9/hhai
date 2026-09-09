package tech.aiflowy.skill.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import tech.aiflowy.common.entity.DateEntity;


public class SkillBase extends DateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId", comment = "主键ID")
    private BigInteger id;

    /**
     * 技能标题
     */
    @Column(comment = "技能标题")
    private String title;

    /**
     * 技能名称
     */
    @Column(comment = "技能名称")
    private String name;

    /**
     * 英文名称
     */
    @Column(comment = "英文名称")
    private String englishName;

    /**
     * 作者
     */
    @Column(comment = "作者")
    private String author;

    /**
     * 技能描述
     */
    @Column(comment = "技能描述")
    private String description;

    /**
     * 分类ID
     */
    @Column(comment = "分类ID")
    private BigInteger categoryId;

    /**
     * 技能版本
     */
    @Column(comment = "技能版本")
    private String version;

    /**
     * 技能摘要(SHA256)
     */
    @Column(comment = "技能摘要(SHA256)")
    private String digest;

    /**
     * 存储键(相对路径)
     */
    @Column(comment = "存储键(相对路径)")
    private String storageKey;

    /**
     * 技能路径
     */
    @Column(comment = "技能路径")
    private String skillPath;

    /**
     * 技能大小
     */
    @Column(comment = "技能大小")
    private Long skillSize;

    /**
     * 文件数量
     */
    @Column(comment = "文件数量")
    private Integer fileCount;

    /**
     * 图标
     */
    @Column(comment = "图标")
    private String icon;

    /**
     * 状态（1:启用 0:禁用）
     */
    @Column(comment = "状态（1:启用 0:禁用）")
    private Integer status;

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
     * 创建时间
     */
    @Column(comment = "创建时间")
    private Date created;

    /**
     * 创建者ID
     */
    @Column(comment = "创建者ID")
    private BigInteger createdBy;

    /**
     * 修改时间
     */
    @Column(comment = "修改时间")
    private Date modified;

    /**
     * 修改者ID
     */
    @Column(comment = "修改者ID")
    private BigInteger modifiedBy;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigInteger getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(BigInteger categoryId) {
        this.categoryId = categoryId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getSkillPath() {
        return skillPath;
    }

    public void setSkillPath(String skillPath) {
        this.skillPath = skillPath;
    }

    public Long getSkillSize() {
        return skillSize;
    }

    public void setSkillSize(Long skillSize) {
        this.skillSize = skillSize;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

}
