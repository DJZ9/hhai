package tech.aiflowy.ai.asynctask;

import com.agentsflex.asynctask.AsyncTask;
import com.agentsflex.asynctask.AsyncTaskManager;
import com.agentsflex.asynctask.AsyncTaskStatus;
import com.agentsflex.core.model.ocr.OcrResponse;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.config.AsyncTaskProperties;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.math.BigInteger;
import java.util.Map;

/**
 * OCR 异步任务对外服务：提交任务到 Redis 队列、阻塞等待结果、查询任务状态。
 */
@Component
public class OcrTaskService {

    private final AsyncTaskManager manager;
    private final AsyncTaskProperties properties;

    public OcrTaskService(AsyncTaskManager manager, AsyncTaskProperties properties) {
        this.manager = manager;
        this.properties = properties;
    }

    /**
     * 提交 OCR 任务到队列，返回框架 taskId。
     */
    public String submit(BigInteger modelId, String fileUrl, Map<String, Object> options, long trackingTimeoutMillis) {
        OcrTaskParams params = new OcrTaskParams(modelId, fileUrl, options);
        return manager.submit(params, trackingTimeoutMillis).getId();
    }

    /**
     * 使用默认跟踪超时提交 OCR 任务。
     */
    public String submit(BigInteger modelId, String fileUrl, Map<String, Object> options) {
        return submit(modelId, fileUrl, options, properties.getTrackingTimeoutMillis());
    }

    /**
     * 阻塞等待任务终态，返回 OcrResponse（markdown 优先于 text）；失败/超时抛业务异常。
     */
    public OcrResponse awaitResult(String taskId, long timeoutMillis, long intervalMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            AsyncTask task = manager.get(taskId);
            if (task == null) {
                throw new BusinessException("OCR 任务不存在: " + taskId);
            }
            AsyncTaskStatus status = task.getStatus();
            if (status != null && status.isTerminal()) {
                if (status == AsyncTaskStatus.SUCCEEDED) {
                    Object result = task.getResult();
                    if (result instanceof OcrResponse) {
                        return (OcrResponse) result;
                    }
                    throw new BusinessException("OCR 任务成功但结果为空");
                }
                throw new BusinessException(task.getErrorMessage() != null
                        ? task.getErrorMessage()
                        : "OCR 任务失败: " + status);
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new BusinessException("OCR 任务超时");
            }
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("OCR 等待被中断");
            }
        }
    }

    /**
     * 查询任务最新状态
     */
    public AsyncTask get(String taskId) {
        return manager.get(taskId);
    }

    public AsyncTaskManager getManager() {
        return manager;
    }
}
