package tech.aiflowy.ai.tinyflow.event;

import jakarta.annotation.Resource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.entity.WorkflowBatchPre;
import tech.aiflowy.ai.service.WorkflowBatchPreService;

import java.math.BigInteger;

@Component
public class PreWorkflowEvent {

    @Resource
    private WorkflowBatchPreService workflowBatchPreService;

    @Async
    @EventListener
    public void handlePreWorkflowEvent(PreEventEntity event) {
        BigInteger preId = event.getPreId();
        Integer status = event.getStatus();
        if (preId != null) {
            WorkflowBatchPre update = new WorkflowBatchPre();
            update.setId(preId);
            update.setStatus(status);
            update.setOutput(event.getOutput());
            update.setStartTime(event.getStartTime());
            update.setEndTime(event.getEndTime());
            workflowBatchPreService.updateById(update);
        }
    }
}
