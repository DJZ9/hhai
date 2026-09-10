package tech.aiflowy.common.third.auth.entity;

import com.alibaba.fastjson2.JSONObject;

public class PlatformUser {

    /**
     * 平台用户唯一标识
     */
    private String userKey;
    private String username;
    private String nickname;
    private String avatar;

    /**
     * 平台原始数据
     */
    private JSONObject origData;

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public JSONObject getOrigData() {
        return origData;
    }

    public void setOrigData(JSONObject origData) {
        this.origData = origData;
    }
}
