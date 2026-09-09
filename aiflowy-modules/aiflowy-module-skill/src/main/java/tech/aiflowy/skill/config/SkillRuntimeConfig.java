package tech.aiflowy.skill.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Skill Runtime 配置类
 * 从 application.yml 读取配置
 *
 * @author michael
 * @since 2025-07-21
 */
@Configuration
@ConfigurationProperties(prefix = "aiflowy.skill.runtime")
public class SkillRuntimeConfig {

    /**
     * Runtime 类型: local
     */
    private String type = "local";

    /**
     * Local 配置
     */
    private LocalConfig local = new LocalConfig();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalConfig getLocal() {
        return local;
    }

    public void setLocal(LocalConfig local) {
        this.local = local;
    }

    /**
     * Local Runtime 配置
     */
    public static class LocalConfig {

        private String conversationsRoot;

        public String getConversationsRoot() {
            return conversationsRoot;
        }

        public void setConversationsRoot(String conversationsRoot) {
            this.conversationsRoot = conversationsRoot;
        }
    }
}