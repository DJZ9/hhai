package tech.aiflowy.ai.dto;

import jakarta.validation.constraints.NotEmpty;

/**
 * 获取MCP工具请求
 */
public class GetMcpToolsRequestDTO {

    /**
     * MCP ID
     */
    @NotEmpty(message = "id不能为空")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
