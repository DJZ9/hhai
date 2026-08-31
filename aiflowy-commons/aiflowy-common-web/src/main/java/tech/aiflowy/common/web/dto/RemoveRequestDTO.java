package tech.aiflowy.common.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;

/**
 * 删除请求DTO
 */
public class RemoveRequestDTO {

    /**
     * 主键ID
     */
    @NotEmpty(message = "id不能为空")
    private Serializable id;

    public Serializable getId() {
        return id;
    }

    public void setId(Serializable id) {
        this.id = id;
    }
}
