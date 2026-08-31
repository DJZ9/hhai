package tech.aiflowy.ai.tinyflow.repository;

import dev.tinyflow.core.chain.ChainDefinition;
import dev.tinyflow.core.chain.repository.ChainDefinitionRepository;
import dev.tinyflow.core.parser.ChainParser;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.entity.Workflow;
import tech.aiflowy.ai.service.WorkflowService;

import javax.annotation.Resource;

@Component
public class ChainDefinitionRepositoryImpl implements ChainDefinitionRepository {

    @Resource
    private WorkflowService workflowService;
    @Resource
    private ChainParser chainParser;

    @Override
    public ChainDefinition getChainDefinitionById(String id) {
        Workflow workflow = workflowService.getById(id);
        String json = workflow.getContent();
        ChainDefinition chainDefinition = chainParser.parse(json);
        chainDefinition.setId(workflow.getId().toString());
        chainDefinition.setName(workflow.getEnglishName());
        chainDefinition.setDescription(workflow.getDescription());
        return chainDefinition;
    }
}
