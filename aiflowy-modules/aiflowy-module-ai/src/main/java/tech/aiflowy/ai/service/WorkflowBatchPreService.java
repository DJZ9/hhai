package tech.aiflowy.ai.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.ai.entity.WorkflowBatchPre;

import java.math.BigInteger;

/**
 * 批量预执行列表 服务层。
 *
 * @author John
 * @since 2026-05-18
 */
public interface WorkflowBatchPreService extends IService<WorkflowBatchPre> {

    void removePreRecord(BigInteger id);

}
