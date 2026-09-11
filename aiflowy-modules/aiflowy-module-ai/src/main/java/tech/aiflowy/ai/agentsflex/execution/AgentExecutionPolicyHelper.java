package tech.aiflowy.ai.agentsflex.execution;

import com.agentsflex.agent.AgentBudget;
import com.agentsflex.agent.AgentExecutionPolicy;
import com.agentsflex.agent.AgentParallelFailureStrategy;
import com.agentsflex.agent.AgentRetryPolicy;
import com.agentsflex.agent.AgentSuspensionExpirationStrategy;
import com.agentsflex.agent.AgentToolExecutionMode;
import com.agentsflex.agent.AgentToolResultOverflowStrategy;
import com.agentsflex.agent.tool.ToolErrorStrategy;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.entity.Bot;

/**
 * Agent 执行策略构建助手。
 * <p>
 * 从 Bot 配置构建 AgentExecutionPolicy，统一三处 Agent 构建逻辑，避免重复代码。
 * 复用 Bot 实体中的 getter 方法读取 modelOptions 中的高级执行配置。
 */
@Component
public class AgentExecutionPolicyHelper {

    /**
     * 从 Bot 配置构建 Agent 执行策略
     * 复用 Bot 实体中的 getter 方法读取 modelOptions 中的高级执行配置，
     * 统一三处 Agent 构建逻辑，避免重复代码。
     * @param aiBot Bot 实体，为 null 时返回默认策略
     * @return AgentExecutionPolicy 对象
     */
    public AgentExecutionPolicy buildExecutionPolicy(Bot aiBot) {
        if (aiBot == null) {
            // Bot 不存在时使用默认策略
            return AgentExecutionPolicy.builder()
                    .maxIterations(200)
                    .maxSteps(1000)
                    .budget(AgentBudget.builder()
                            .maxDurationMillis(20 * 60 * 1000)
                            .build())
                    .toolErrorStrategy(ToolErrorStrategy.RETURN_ERROR_TO_MODEL)
                    .build();
        }

        // 构建 AgentBudget，所有值为0表示不限制
        AgentBudget.Builder budgetBuilder = AgentBudget.builder()
                .maxInputTokens(aiBot.getMaxInputTokens())
                .maxOutputTokens(aiBot.getMaxOutputTokens())
                .maxTotalTokens(aiBot.getMaxTotalTokens())
                .maxToolCalls(aiBot.getMaxToolCalls())
                .maxDurationMillis(aiBot.getMaxDurationMillis());

        // 构建 AgentExecutionPolicy，从 Bot 配置读取全部执行参数
        AgentExecutionPolicy.Builder policyBuilder = AgentExecutionPolicy.builder()
                .maxIterations(aiBot.getMaxIterations())
                .maxSteps(aiBot.getMaxSteps())
                .budget(budgetBuilder.build())
                .modelCallTimeoutMillis(aiBot.getModelCallTimeoutMillis())
                .toolExecutionTimeoutMillis(aiBot.getToolExecutionTimeoutMillis())
                .externalToolTimeoutMillis(aiBot.getExternalToolTimeoutMillis())
                .approvalTimeoutMillis(aiBot.getApprovalTimeoutMillis())
                .userInputTimeoutMillis(aiBot.getUserInputTimeoutMillis())
                .toolResultMaxCharacters(aiBot.getToolResultMaxCharacters())
                .externalToolResultMaxCharacters(aiBot.getExternalToolResultMaxCharacters())
                .maxParallelToolCalls(aiBot.getMaxParallelToolCalls())
                .interruptedToolMessageTemplate(aiBot.getInterruptedToolMessageTemplate())
                .interruptedTurnMessageTemplate(aiBot.getInterruptedTurnMessageTemplate())
                .cancellationReason(aiBot.getCancellationReason());

        // 设置工具错误策略
        String toolErrorStrategy = aiBot.getToolErrorStrategy();
        if ("FAIL_RUN".equals(toolErrorStrategy)) {
            policyBuilder.toolErrorStrategy(ToolErrorStrategy.FAIL_RUN);
        } else {
            policyBuilder.toolErrorStrategy(ToolErrorStrategy.RETURN_ERROR_TO_MODEL);
        }

        // 设置挂起过期策略
        String suspensionStrategy = aiBot.getSuspensionExpirationStrategy();
        if ("FAIL_TURN".equals(suspensionStrategy)) {
            policyBuilder.suspensionExpirationStrategy(AgentSuspensionExpirationStrategy.FAIL_TURN);
        } else if ("CANCEL_TURN".equals(suspensionStrategy)) {
            policyBuilder.suspensionExpirationStrategy(AgentSuspensionExpirationStrategy.CANCEL_TURN);
        } else {
            policyBuilder.suspensionExpirationStrategy(AgentSuspensionExpirationStrategy.REJECT_RESUME);
        }

        // 设置工具结果超限策略
        String overflowStrategy = aiBot.getToolResultOverflowStrategy();
        if ("TRUNCATE".equals(overflowStrategy)) {
            policyBuilder.toolResultOverflowStrategy(AgentToolResultOverflowStrategy.TRUNCATE);
        } else {
            policyBuilder.toolResultOverflowStrategy(AgentToolResultOverflowStrategy.FAIL);
        }

        // 设置工具执行模式
        String executionMode = aiBot.getToolExecutionMode();
        if ("PARALLEL".equals(executionMode)) {
            policyBuilder.toolExecutionMode(AgentToolExecutionMode.PARALLEL);
        } else {
            policyBuilder.toolExecutionMode(AgentToolExecutionMode.SEQUENTIAL);
        }

        // 设置并行失败策略
        String parallelFailure = aiBot.getParallelFailureStrategy();
        if ("RETURN_ERRORS_TO_MODEL".equals(parallelFailure)) {
            policyBuilder.parallelFailureStrategy(AgentParallelFailureStrategy.RETURN_ERRORS_TO_MODEL);
        } else {
            policyBuilder.parallelFailureStrategy(AgentParallelFailureStrategy.FAIL_FAST);
        }

        // 设置重试策略，仅在 maxRetries > 0 时启用，避免无效重试配置导致构建异常
        int maxRetries = aiBot.getMaxRetries();
        if (maxRetries > 0) {
            long initialDelay = aiBot.getInitialDelayMillis();
            if (initialDelay <= 0) {
                initialDelay = 1000;
            }
            long maxDelay = aiBot.getMaxDelayMillis();
            if (maxDelay < initialDelay) {
                maxDelay = initialDelay;
            }
            double multiplier = aiBot.getRetryMultiplier();
            if (multiplier < 1.0) {
                multiplier = 2.0;
            }
            policyBuilder.retryPolicy(AgentRetryPolicy.builder()
                    .maxRetries(maxRetries)
                    .initialDelayMillis(initialDelay)
                    .maxDelayMillis(maxDelay)
                    .multiplier(multiplier)
                    .build());
        }

        return policyBuilder.build();
    }
}
