package tech.aiflowy.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 管理员目录配置。
 */
@Configuration
@ConfigurationProperties(prefix = "aiflowy.mcp")
public class McpAdminCatalogProperties {

    /**
     * 管理员托管的非 HTTP/本地 MCP 配置目录。
     */
    private String adminConfigDir = "";

    /**
     * classpath 中的管理员 MCP 清单文件。
     */
    private String adminConfigClasspath = "mcp/aiflowy-mcp-catalog.json";

    public String getAdminConfigDir() {
        return adminConfigDir;
    }

    public void setAdminConfigDir(String adminConfigDir) {
        this.adminConfigDir = adminConfigDir;
    }

    public String getAdminConfigClasspath() {
        return adminConfigClasspath;
    }

    public void setAdminConfigClasspath(String adminConfigClasspath) {
        this.adminConfigClasspath = adminConfigClasspath;
    }
}
