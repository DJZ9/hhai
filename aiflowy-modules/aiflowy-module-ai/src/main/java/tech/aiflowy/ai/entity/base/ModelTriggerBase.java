package tech.aiflowy.ai.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;


public class ModelTriggerBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId", comment = "主键ID")
    private BigInteger id;

    /**
     * 租户ID
     */
    @Column(tenantId = true, comment = "租户ID")
    private BigInteger tenantId;

    /**
     * 部门ID
     */
    @Column(comment = "部门ID")
    private BigInteger deptId;

    /**
     * 模型ID
     */
    @Column(comment = "模型ID")
    private BigInteger modelId;

    /**
     * 模型供应商
     */
    @Column(comment = "模型供应商")
    private String provider;

    /**
     * 模型名称
     */
    @Column(comment = "模型名称")
    private String modelName;

    /**
     * 消耗tokens
     */
    @Column(comment = "消耗tokens")
    private Integer totalTokens;

    /**
     * 对话耗时
     */
    @Column(comment = "对话耗时")
    private Long chatTime;

    /**
     * 流式请求耗时
     */
    @Column(comment = "流式请求耗时")
    private Long streamTime;

    /**
     * 非流式请求耗时
     */
    @Column(comment = "非流式请求耗时")
    private Long nonStreamTime;

    /**
     * 工作流ID
     */
    @Column(comment = "工作流ID")
    private BigInteger workflowId;

    /**
     * BOT ID
     */
    @Column(comment = "BOT ID")
    private BigInteger botId;

    /**
     * 用户ID
     */
    @Column(comment = "用户ID")
    private BigInteger accountId;

    /**
     * 创建时间
     */
    @Column(comment = "创建时间")
    private Date created;

    /**
     * 创建日期
     */
    @Column(comment = "创建日期")
    private Date createdDay;

    /**
     * 创建者ID
     */
    @Column(comment = "创建者ID")
    private BigInteger createdBy;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public BigInteger getTenantId() {
        return tenantId;
    }

    public void setTenantId(BigInteger tenantId) {
        this.tenantId = tenantId;
    }

    public BigInteger getDeptId() {
        return deptId;
    }

    public void setDeptId(BigInteger deptId) {
        this.deptId = deptId;
    }

    public BigInteger getModelId() {
        return modelId;
    }

    public void setModelId(BigInteger modelId) {
        this.modelId = modelId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Long getChatTime() {
        return chatTime;
    }

    public void setChatTime(Long chatTime) {
        this.chatTime = chatTime;
    }

    public Long getStreamTime() {
        return streamTime;
    }

    public void setStreamTime(Long streamTime) {
        this.streamTime = streamTime;
    }

    public Long getNonStreamTime() {
        return nonStreamTime;
    }

    public void setNonStreamTime(Long nonStreamTime) {
        this.nonStreamTime = nonStreamTime;
    }

    public BigInteger getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(BigInteger workflowId) {
        this.workflowId = workflowId;
    }

    public BigInteger getBotId() {
        return botId;
    }

    public void setBotId(BigInteger botId) {
        this.botId = botId;
    }

    public BigInteger getAccountId() {
        return accountId;
    }

    public void setAccountId(BigInteger accountId) {
        this.accountId = accountId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getCreatedDay() {
        return createdDay;
    }

    public void setCreatedDay(Date createdDay) {
        this.createdDay = createdDay;
    }

    public BigInteger getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(BigInteger createdBy) {
        this.createdBy = createdBy;
    }

}
