package tech.aiflowy.common.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.util.Collection;

/**
 * 批量删除请求DTO
 */
public class RemoveBatchRequestDTO {

    /**
     * 主键ID集合
     */
    @NotEmpty(message = "ids不能为空")
    private Collection<Serializable> ids;

    public Collection<Serializable> getIds() {
        return ids;
    }

    public void setIds(Collection<Serializable> ids) {
        this.ids = ids;
    }
}
