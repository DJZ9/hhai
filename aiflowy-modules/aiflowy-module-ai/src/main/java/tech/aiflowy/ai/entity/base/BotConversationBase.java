package tech.aiflowy.ai.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.handler.FastjsonTypeHandler;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import tech.aiflowy.common.entity.DateEntity;


public class BotConversationBase extends DateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话id
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId", comment = "会话id")
    private BigInteger id;

    /**
     * 会话标题
     */
    @Column(comment = "会话标题")
    private String title;

    /**
     * botid
     */
    @Column(comment = "botid")
    private BigInteger botId;

    /**
     * 账户 id
     */
    @Column(comment = "账户 id")
    private BigInteger accountId;

    /**
     * 创建时间
     */
    @Column(comment = "创建时间")
    private Date created;

    /**
     * 创建者Id
     */
    @Column(comment = "创建者Id")
    private BigInteger createdBy;

    /**
     * 修改者
     */
    @Column(comment = "修改者")
    private Date modified;

    /**
     * 修改者id
     */
    @Column(comment = "修改者id")
    private BigInteger modifiedBy;

    /**
     * 选项
     */
    @Column(typeHandler = FastjsonTypeHandler.class, comment = "选项")
    private Map<String, Object> options;

    /**
     * 系统提示词
     */
    @Column(comment = "系统提示词")
    private String systemPrompt;

    /**
     * 用户变量表单
     */
    @Column(typeHandler = FastjsonTypeHandler.class, comment = "用户变量表单")
    private Map<String, Object> variablesFormOptions;

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

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Map<String, Object> getVariablesFormOptions() {
        return variablesFormOptions;
    }

    public void setVariablesFormOptions(Map<String, Object> variablesFormOptions) {
        this.variablesFormOptions = variablesFormOptions;
    }

}
