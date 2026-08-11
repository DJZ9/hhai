package tech.aiflowy.ai.entity;

import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.entity.base.WorkflowBatchPreBase;


/**
 * 批量预执行列表 实体类。
 *
 * @author John
 * @since 2026-05-18
 */
@Table(value = "tb_workflow_batch_pre", comment = "批量预执行列表")
public class WorkflowBatchPre extends WorkflowBatchPreBase {

    public Long getExecTime() {
        if (getEndTime() == null) {
            return null;
        }
        return getEndTime().getTime() - getStartTime().getTime();
    }

}
