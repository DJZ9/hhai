package tech.aiflowy.usercenter.controller.ai;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.ai.entity.WorkflowExecResult;
import tech.aiflowy.ai.entity.WorkflowExecStep;
import tech.aiflowy.ai.service.WorkflowExecResultService;
import tech.aiflowy.ai.service.WorkflowExecStepService;
import tech.aiflowy.ai.utils.WorkFlowUtil;
import tech.aiflowy.common.annotation.UsePermission;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.web.controller.BaseCurdController;

import javax.annotation.Resource;
import java.math.BigInteger;

/**
 * 工作流执行记录
 */
@RestController
@RequestMapping("/userCenter/workflowExecResult")
@UsePermission(moduleName = "/api/v1/workflow")
public class UcWorkflowExecResultController extends BaseCurdController<WorkflowExecResultService, WorkflowExecResult> {

    @Resource
    private WorkflowExecStepService recordStepService;

    public UcWorkflowExecResultController(WorkflowExecResultService service) {
        super(service);
    }

    /**
     * 删除
     */
    @GetMapping("/del")
    @Transactional(rollbackFor = Exception.class)
    @SaCheckPermission("/api/v1/workflow/remove")
    public Result<Void> del(BigInteger id) {
        LoginAccount account = SaTokenUtil.getLoginAccount();
        WorkflowExecResult record = service.getById(id);
        if (!account.getId().toString().equals(record.getCreatedBy())) {
            return Result.fail(1, "非法请求");
        }
        service.removeById(id);
        QueryWrapper w = QueryWrapper.create();
        w.eq(WorkflowExecStep::getRecordId, id);
        recordStepService.remove(w);
        return Result.ok();
    }

    @GetMapping("getPage")
    public Result<Page<WorkflowExecResult>> getPage(HttpServletRequest request,
                                                    String sortKey,
                                                    String sortType,
                                                    Long pageNumber,
                                                    Long pageSize,
                                                    String queryBegin,
                                                    String queryEnd) {
        if (pageNumber == null || pageNumber < 1) {
            pageNumber = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }

        QueryWrapper queryWrapper = buildQueryWrapper(request);
        if (StrUtil.isNotEmpty(queryBegin) && StrUtil.isNotEmpty(queryEnd)) {
            queryWrapper.between(WorkflowExecResult::getStartTime, queryBegin, queryEnd);
        }
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        return Result.ok(queryPage(new Page<>(pageNumber, pageSize), queryWrapper));
    }

    @Override
    protected Page<WorkflowExecResult> queryPage(Page<WorkflowExecResult> page, QueryWrapper queryWrapper) {
        queryWrapper.eq(WorkflowExecResult::getCreatedBy, SaTokenUtil.getLoginAccount().getId().toString());
        Page<WorkflowExecResult> res = super.queryPage(page, queryWrapper);
        for (WorkflowExecResult record : res.getRecords()) {
            record.setWorkflowJson(WorkFlowUtil.removeSensitiveInfo(record.getWorkflowJson()));
        }
        return res;
    }
}