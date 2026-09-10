package tech.aiflowy.common.third.auth.entity;

/**
 * 第三方平台简要信息（不含敏感配置）
 */
public class OAuthPlatformVo {

    private String id;
    private String platform;
    private String displayName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
}
