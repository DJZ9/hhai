package tech.aiflowy.datacenter.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.datacenter.entity.DatacenterTableParseTask;
import tech.aiflowy.datacenter.mapper.DatacenterTableParseTaskMapper;
import tech.aiflowy.datacenter.service.DatacenterTableParseTaskService;

import java.math.BigInteger;
import java.util.Date;

/**
 * 数据中枢表解析任务 Service 实现
 */
@Service
public class DatacenterTableParseTaskServiceImpl extends ServiceImpl<DatacenterTableParseTaskMapper, DatacenterTableParseTask> implements DatacenterTableParseTaskService {

    @Override
    public BigInteger createTask(String taskName, String taskType, String inputData, BigInteger deptId, BigInteger tenantId, BigInteger createdBy) {
        DatacenterTableParseTask task = new DatacenterTableParseTask();
        task.setTaskName(taskName);
        task.setTaskType(taskType);
        task.setStatus(DatacenterTableParseTask.STATUS_PROCESSING);
        task.setInputData(inputData);
        task.setProgress(0);
        task.setProgressMessage("任务创建中...");
        task.setDeptId(deptId);
        task.setTenantId(tenantId);
        task.setCreated(new Date());
        task.setCreatedBy(createdBy);
        task.setUpdated(new Date());

        this.save(task);
        return task.getId();
    }

    @Override
    public void updateProgress(BigInteger taskId, int progress, String message) {
        DatacenterTableParseTask task = new DatacenterTableParseTask();
        task.setId(taskId);
        task.setProgress(progress);
        task.setProgressMessage(message);
        task.setUpdated(new Date());

        this.updateById(task);
    }

    @Override
    public void updateSuccess(BigInteger taskId, String resultData) {
        DatacenterTableParseTask task = new DatacenterTableParseTask();
        task.setId(taskId);
        task.setStatus(DatacenterTableParseTask.STATUS_SUCCESS);
        task.setProgress(100);
        task.setProgressMessage("解析完成");
        task.setResultData(resultData);
        task.setUpdated(new Date());

        this.updateById(task);
    }

    @Override
    public void updateFailed(BigInteger taskId, String errorInfo) {
        DatacenterTableParseTask task = new DatacenterTableParseTask();
        task.setId(taskId);
        task.setStatus(DatacenterTableParseTask.STATUS_FAILED);
        task.setErrorInfo(errorInfo);
        task.setUpdated(new Date());

        this.updateById(task);
    }

    @Override
    public DatacenterTableParseTask getTaskStatus(BigInteger taskId) {
        return this.getById(taskId);
    }
}
