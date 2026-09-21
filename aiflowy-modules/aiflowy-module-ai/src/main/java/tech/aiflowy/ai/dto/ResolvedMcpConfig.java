package tech.aiflowy.ai.dto;

/**
 * 运行时解析后的 MCP 配置。
 */
public class ResolvedMcpConfig {

    private String managedServerName;
    private String runtimeConfigJson;
    private boolean shared;
    private String catalogKey;
    private String protocol;

    public String getManagedServerName() {
        return managedServerName;
    }

    public void setManagedServerName(String managedServerName) {
        this.managedServerName = managedServerName;
    }

    public String getRuntimeConfigJson() {
        return runtimeConfigJson;
    }

    public void setRuntimeConfigJson(String runtimeConfigJson) {
        this.runtimeConfigJson = runtimeConfigJson;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getCatalogKey() {
        return catalogKey;
    }

    public void setCatalogKey(String catalogKey) {
        this.catalogKey = catalogKey;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
