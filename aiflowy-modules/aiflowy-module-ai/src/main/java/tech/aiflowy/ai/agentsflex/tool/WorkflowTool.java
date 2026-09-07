package tech.aiflowy.ai.agentsflex.tool;

import com.agentsflex.core.model.chat.tool.BaseTool;
import com.agentsflex.core.model.chat.tool.Parameter;
import com.agentsflex.core.prompt.PromptTemplate;
import dev.tinyflow.core.chain.ChainDefinition;
import dev.tinyflow.core.chain.DataType;
import dev.tinyflow.core.chain.runtime.ChainExecutor;
import tech.aiflowy.ai.entity.Workflow;
import tech.aiflowy.common.util.SpringContextUtil;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WorkflowTool extends BaseTool {

    private BigInteger workflowId;
    private String botOutputTemplate;

    public WorkflowTool() {
    }

    public WorkflowTool(Workflow workflow, boolean needEnglishName) {
        this.workflowId = workflow.getId();
        if (needEnglishName) {
            this.name = workflow.getEnglishName();
        } else {
            this.name = workflow.getTitle();
        }
        this.description = workflow.getDescription();
        this.parameters = toParameters(workflow);
    }


    static Parameter[] toParameters(Workflow workflow) {
        ChainExecutor executor = SpringContextUtil.getBean(ChainExecutor.class);
        ChainDefinition definition = executor.getDefinitionRepository().getChainDefinitionById(workflow.getId().toString());
        List<dev.tinyflow.core.chain.Parameter> parameterDefs = definition.getStartParameters();
        if (parameterDefs == null || parameterDefs.isEmpty()) {
            return new Parameter[0];
        }

        Parameter[] parameters = new Parameter[parameterDefs.size()];
        for (int i = 0; i < parameterDefs.size(); i++) {
            dev.tinyflow.core.chain.Parameter parameterDef = parameterDefs.get(i);
            Parameter parameter = new Parameter();
            parameter.setName(parameterDef.getName());
            parameter.setDescription(parameterDef.getDescription());
            DataType dataType = parameterDef.getDataType();
            if (dataType == null) dataType = DataType.String;
            parameter.setType(dataType.toString().toLowerCase());
            parameter.setRequired(parameterDef.isRequired());
            parameters[i] = parameter;
        }
        return parameters;
    }

    public BigInteger getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(BigInteger workflowId) {
        this.workflowId = workflowId;
    }

    public String getBotOutputTemplate() {
        return botOutputTemplate;
    }

    public void setBotOutputTemplate(String botOutputTemplate) {
        this.botOutputTemplate = botOutputTemplate;
    }

    @Override
    public Object invoke(Map<String, Object> argsMap) {
        ChainExecutor executor = SpringContextUtil.getBean(ChainExecutor.class);
        Object result = executor.execute(workflowId.toString(), argsMap);
        // 应用Bot自定义输出模板
        if (botOutputTemplate != null && !botOutputTemplate.isEmpty() && result != null) {
            PromptTemplate promptTemplate = PromptTemplate.of(botOutputTemplate);
            promptTemplate.setKeepExpressionOnMissingVariable(true);
            String formattedOutput = promptTemplate.format((Map<String, Object>) result);
            return formattedOutput;
        }
        return result;
    }

    @Override
    public String toString() {
        return "AiWorkflowFunction{" +
                "workflowId=" + workflowId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parameters=" + Arrays.toString(parameters) +
                '}';
    }
}
