package tech.aiflowy.ai.dto;

/**
 * 前端 MCP 目录列表项，不包含命令、参数或密钥。
 */
public class McpCatalogItemDTO {

    private String key;
    private String title;
    private String description;
    private String protocol;
    private boolean running;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
