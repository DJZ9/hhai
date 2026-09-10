package tech.aiflowy.system.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.handler.FastjsonTypeHandler;
import tech.aiflowy.common.entity.DateEntity;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;


public class SysAccountOpenidBase extends DateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto, value = "snowFlakeId")
    private BigInteger id;

    /**
     * 用户ID
     */
    @Column(comment = "用户ID")
    private BigInteger accountId;

    /**
     * 第三方类型：wechat，dingding，qq...
     */
    @Column(comment = "第三方类型：wechat，dingding，qq...")
    private String openidType;

    /**
     * 第三方的openId的值
     */
    @Column(comment = "第三方的openId的值")
    private String openidValue;

    /**
     * 昵称
     */
    @Column(comment = "昵称")
    private String nickname;

    /**
     * 头像
     */
    @Column(comment = "头像")
    private String avatar;

    @Column(typeHandler = FastjsonTypeHandler.class)
    private Map<String, Object> options;

    private Date created;

    private Date modified;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public BigInteger getAccountId() {
        return accountId;
    }

    public void setAccountId(BigInteger accountId) {
        this.accountId = accountId;
    }

    public String getOpenidType() {
        return openidType;
    }

    public void setOpenidType(String openidType) {
        this.openidType = openidType;
    }

    public String getOpenidValue() {
        return openidValue;
    }

    public void setOpenidValue(String openidValue) {
        this.openidValue = openidValue;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

}
