package tech.aiflowy.datacenter.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.datacenter.entity.DatacenterTableParseTask;

import java.math.BigInteger;

/**
 * 数据中枢表解析任务 Service
 */
public interface DatacenterTableParseTaskService extends IService<DatacenterTableParseTask> {

    /**
     * 创建解析任务
     */
    BigInteger createTask(String taskName, String taskType, String inputData, BigInteger deptId, BigInteger tenantId, BigInteger createdBy);

    /**
     * 更新任务进度
     */
    void updateProgress(BigInteger taskId, int progress, String message);

    /**
     * 更新任务成功
     */
    void updateSuccess(BigInteger taskId, String resultData);

    /**
     * 更新任务失败
     */
    void updateFailed(BigInteger taskId, String errorInfo);

    /**
     * 获取任务状态
     */
    DatacenterTableParseTask getTaskStatus(BigInteger taskId);
}
