package tech.aiflowy.skill.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;


public class BotSkillBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto, value = "snowFlakeId", comment = "主键ID")
    private BigInteger id;

    /**
     * Bot ID
     */
    @Column(comment = "Bot ID")
    private BigInteger botId;

    /**
     * 技能ID
     */
    @Column(comment = "技能ID")
    private BigInteger skillId;

    /**
     * 创建时间
     */
    @Column(comment = "创建时间")
    private Date createdAt;

    /**
     * 租户ID
     */
    @Column(tenantId = true, comment = "租户ID")
    private BigInteger tenantId;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public BigInteger getBotId() {
        return botId;
    }

    public void setBotId(BigInteger botId) {
        this.botId = botId;
    }

    public BigInteger getSkillId() {
        return skillId;
    }

    public void setSkillId(BigInteger skillId) {
        this.skillId = skillId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public BigInteger getTenantId() {
        return tenantId;
    }

    public void setTenantId(BigInteger tenantId) {
        this.tenantId = tenantId;
    }

}
