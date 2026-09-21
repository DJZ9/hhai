package tech.aiflowy.ai.dto;

/**
 * 从管理员目录安装 MCP 的请求。
 */
public class McpCatalogImportDTO {

    private String catalogKey;
    private String title;
    private String description;

    public String getCatalogKey() {
        return catalogKey;
    }

    public void setCatalogKey(String catalogKey) {
        this.catalogKey = catalogKey;
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
}
