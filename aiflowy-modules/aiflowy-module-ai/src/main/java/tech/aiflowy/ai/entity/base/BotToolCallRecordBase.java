package tech.aiflowy.ai.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;


public class BotToolCallRecordBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId", comment = "主键ID")
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
     * 智能体ID
     */
    @Column(comment = "智能体ID")
    private BigInteger botId;

    /**
     * 智能体名称
     */
    @Column(comment = "智能体名称")
    private String botTitle;

    /**
     * 会话ID
     */
    @Column(comment = "会话ID")
    private BigInteger conversationId;

    /**
     * 工具名称
     */
    @Column(comment = "工具名称")
    private String toolName;

    /**
     * 工具调用入参(JSON)
     */
    @Column(comment = "工具调用入参(JSON)")
    private String inputParams;

    /**
     * 工具调用输出(JSON)
     */
    @Column(comment = "工具调用输出(JSON)")
    private String outputParams;

    /**
     * 调用时间
     */
    @Column(comment = "调用时间")
    private Date created;

    /**
     * 创建人ID
     */
    @Column(comment = "创建人ID")
    private BigInteger createdBy;

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

    public BigInteger getBotId() {
        return botId;
    }

    public void setBotId(BigInteger botId) {
        this.botId = botId;
    }

    public String getBotTitle() {
        return botTitle;
    }

    public void setBotTitle(String botTitle) {
        this.botTitle = botTitle;
    }

    public BigInteger getConversationId() {
        return conversationId;
    }

    public void setConversationId(BigInteger conversationId) {
        this.conversationId = conversationId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getInputParams() {
        return inputParams;
    }

    public void setInputParams(String inputParams) {
        this.inputParams = inputParams;
    }

    public String getOutputParams() {
        return outputParams;
    }

    public void setOutputParams(String outputParams) {
        this.outputParams = outputParams;
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

}
