package tech.aiflowy.ai.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import tech.aiflowy.ai.entity.WorkflowBatchPre;
import tech.aiflowy.ai.entity.WorkflowExecResult;
import tech.aiflowy.ai.entity.WorkflowExecStep;
import tech.aiflowy.ai.mapper.WorkflowBatchPreMapper;
import tech.aiflowy.ai.mapper.WorkflowExecResultMapper;
import tech.aiflowy.ai.mapper.WorkflowExecStepMapper;
import tech.aiflowy.ai.service.WorkflowBatchPreService;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

/**
 * 批量预执行列表 服务层实现。
 *
 * @author John
 * @since 2026-05-18
 */
@Service
public class WorkflowBatchPreServiceImpl extends ServiceImpl<WorkflowBatchPreMapper, WorkflowBatchPre>  implements WorkflowBatchPreService{

    @Resource
    private WorkflowExecResultMapper execResultMapper;
    @Resource
    private WorkflowExecStepMapper stepMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePreRecord(BigInteger id) {
        removeById(id);
        QueryWrapper ew = QueryWrapper.create();
        ew.eq(WorkflowExecResult::getPreId, id);
        List<WorkflowExecResult> results = execResultMapper.selectListByQuery(ew);
        if (CollectionUtil.isNotEmpty(results)) {
            List<BigInteger> resultIds = results.stream().map(WorkflowExecResult::getId).toList();
            // 删result表
            execResultMapper.deleteByQuery(ew);
            // 删step表
            QueryWrapper rw = QueryWrapper.create();
            rw.in(WorkflowExecStep::getRecordId, resultIds);
            stepMapper.deleteByQuery(rw);
        }
    }

}
