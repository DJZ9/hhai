package tech.aiflowy.ai.config;

/**
 * agents-flex-mcp 默认 classpath 配置的覆盖项。
 */
public final class McpClientManagerDefaults {

    private static final String CONFIG_RESOURCE_PROPERTY = "mcp.config.servers-resource";
    private static final String DISABLED_RESOURCE = "aiflowy-no-builtin-mcp.json";

    private McpClientManagerDefaults() {
    }

    public static void disableBuiltInResource() {
        if (System.getProperty(CONFIG_RESOURCE_PROPERTY) == null) {
            System.setProperty(CONFIG_RESOURCE_PROPERTY, DISABLED_RESOURCE);
        }
    }
}
