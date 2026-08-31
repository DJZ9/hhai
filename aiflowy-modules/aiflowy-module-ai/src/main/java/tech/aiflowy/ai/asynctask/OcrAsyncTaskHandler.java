package tech.aiflowy.ai.asynctask;

import com.agentsflex.asynctask.AsyncTaskStatus;
import com.agentsflex.asynctask.TaskQueryContext;
import com.agentsflex.asynctask.TaskQueryParams;
import com.agentsflex.asynctask.TaskQueryResult;
import com.agentsflex.asynctask.TaskSubmitContext;
import com.agentsflex.asynctask.TaskSubmitResult;
import com.agentsflex.asynctask.handler.AsyncTaskHandler;
import com.agentsflex.core.model.ocr.OcrModel;
import com.agentsflex.core.model.ocr.OcrRequest;
import com.agentsflex.core.model.ocr.OcrResponse;
import com.agentsflex.core.model.ocr.OcrTaskStatus;
import com.mybatisflex.core.tenant.TenantManager;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.common.util.SpringContextUtil;

import java.math.BigInteger;

/**
 * 将项目动态配置的 OCR 模型适配为 AsyncTaskHandler。
 *
 * <p>不在启动时绑定固定模型，而在 submit/query 时按 params 中的 modelId 经
 * {@link ModelService#getModelInstance(BigInteger)} 解析 OcrModel，从而支持 DB 中动态增删的模型。</p>
 */
@Component
public class OcrAsyncTaskHandler implements AsyncTaskHandler<OcrTaskParams> {

    public static final String KEY = "aiflowy:ocr";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Class<OcrTaskParams> getSubmitParamsType() {
        return OcrTaskParams.class;
    }

    @Override
    public void validateSubmitParams(OcrTaskParams params) {
        if (params == null) {
            throw new IllegalArgumentException("OCR 任务参数不能为空");
        }
        if (params.getModelId() == null) {
            throw new IllegalArgumentException("OCR 任务 modelId 不能为空");
        }
        if (params.getFileUrl() == null || params.getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("OCR 任务 fileUrl 不能为空");
        }
    }

    @Override
    public TaskSubmitResult submit(OcrTaskParams params, TaskSubmitContext context) {
        OcrModel ocrModel = resolveModel(params.getModelId());
        OcrRequest request = OcrRequest.ofUrl(params.getFileUrl());
        OcrResponse response = ocrModel.recognize(request);
        TaskSubmitResult result = new TaskSubmitResult();
        apply(response, result);
        if (response != null && response.getTaskId() != null) {
            TaskQueryParams queryParams = new TaskQueryParams(response.getTaskId());
            queryParams.putProviderParam("modelId", params.getModelId());
            result.setQueryParams(queryParams);
        }
        return result;
    }

    @Override
    public TaskQueryResult query(TaskQueryParams params, TaskQueryContext context) {
        BigInteger modelId = extractModelId(params);
        OcrModel ocrModel = resolveModel(modelId);
        OcrResponse response = ocrModel.getResult(params.getExternalTaskId());
        TaskQueryResult result = new TaskQueryResult();
        result.setStatus(response == null ? AsyncTaskStatus.FAILED : map(response.getStatus()));
        result.setProviderStatus(response == null ? null : response.getStatus().name());
        result.setResult(response != null && response.getStatus() == OcrTaskStatus.SUCCEEDED ? response : null);
        result.setErrorCode(response == null ? null : response.getErrorCode());
        result.setErrorMessage(response == null ? "OCR provider returned no response" : response.getErrorMessage());
        return result;
    }

    private OcrModel resolveModel(BigInteger modelId) {
        // 延迟获取 ModelService，避免与 ModelServiceImpl 注入 OcrTaskService 形成 Spring 循环依赖；
        // 本方法仅在 Worker 线程（submit/query）执行，此时 Spring 上下文已就绪。
        ModelService modelService = SpringContextUtil.getBean(ModelService.class);
        if (modelService == null) {
            throw new IllegalStateException("ModelService 未就绪，无法解析 OCR 模型");
        }
        TenantManager.ignoreTenantCondition();
        try {
            Model model = modelService.getModelInstance(modelId);
            if (model == null) {
                throw new IllegalStateException("OCR 模型不存在: " + modelId);
            }
            return model.toOcrModel();
        } finally {
            TenantManager.restoreTenantCondition();
        }
    }

    private BigInteger extractModelId(TaskQueryParams params) {
        Object value = params.getProviderParams().get("modelId");
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }
        if (value instanceof Number) {
            return BigInteger.valueOf(((Number) value).longValue());
        }
        throw new IllegalStateException("OCR 任务查询参数缺少 modelId");
    }

    private AsyncTaskStatus map(OcrTaskStatus status) {
        if (status == null) {
            return AsyncTaskStatus.RUNNING;
        }
        return switch (status) {
            case SUBMITTED -> AsyncTaskStatus.SUBMITTED;
            case SUCCEEDED -> AsyncTaskStatus.SUCCEEDED;
            case FAILED -> AsyncTaskStatus.FAILED;
            case CANCELED -> AsyncTaskStatus.CANCELED;
            case TIMED_OUT -> AsyncTaskStatus.TRACKING_TIMED_OUT;
            default -> AsyncTaskStatus.RUNNING;
        };
    }

    private void apply(OcrResponse response, TaskSubmitResult result) {
        if (response == null) {
            result.setStatus(AsyncTaskStatus.FAILED);
            result.setErrorMessage("OCR provider returned no response");
            return;
        }
        result.setStatus(map(response.getStatus()));
        result.setResult(response.getStatus() == OcrTaskStatus.SUCCEEDED ? response : null);
        result.setErrorCode(response.getErrorCode());
        result.setErrorMessage(response.getErrorMessage());
    }
}
