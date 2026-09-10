package tech.aiflowy.common.third.auth.entity;

import java.util.Date;

/**
 * 第三方平台绑定信息VO
 *
 * @author wgq
 * @since 2026-06-30
 */
public class ThirdPartyBindingVo {

    /**
     * 第三方平台类型
     */
    private String platform;

    /**
     * 平台显示名称
     */
    private String displayName;

    /**
     * 平台图标URL
     */
    private String icon;

    /**
     * 第三方账号昵称
     */
    private String nickname;

    /**
     * 第三方账号头像URL
     */
    private String avatar;

    /**
     * 是否已绑定标识
     */
    private Boolean bound;

    /**
     * 该账号的openId或unionId值
     */
    private String openidValue;

    /**
     * 账号绑定时间
     */
    private Date created;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public Boolean getBound() {
        return bound;
    }

    public void setBound(Boolean bound) {
        this.bound = bound;
    }

    public String getOpenidValue() {
        return openidValue;
    }

    public void setOpenidValue(String openidValue) {
        this.openidValue = openidValue;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}