package tech.aiflowy.ai.agentsflex.compression;

import com.agentsflex.agent.compression.AgentCompressionFailureStrategy;
import com.agentsflex.agent.compression.AgentContextCompressionDecider;
import com.agentsflex.agent.compression.AgentContextCompressionDeciders;
import com.agentsflex.agent.compression.AgentContextCompressionPolicy;
import com.agentsflex.agent.compression.AgentContextCompressionValidator;
import com.agentsflex.agent.compression.AgentContextCompressor;
import com.agentsflex.agent.compression.AgentContextCompressors;
import com.agentsflex.core.message.Message;
import com.agentsflex.core.model.chat.ChatModel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.entity.Bot;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.service.ModelService;

import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Agent 消息压缩助手：封装内存压缩状态存储和增量压缩策略的创建。
 * <p>
 * 按 Turn 触发（阈值 2 轮）、保留最近 2 轮原文、摘要模型使用当前对话模型、
 * 摘要指令使用内置中文规则，用户不可配置；摘要失败时降级为使用未压缩的原始历史。
 * </p>
 */
@Component
public class AgentCompressionHelper {

    private static final Logger log = LoggerFactory.getLogger(AgentCompressionHelper.class);

    /**
     * 简单的 Token 估算器：按消息文本长度的 1/4 估算。
     */
    private static final ToLongFunction<List<Message>> TOKEN_ESTIMATOR = messages -> {
        if (messages == null) return 0;
        long total = 0;
        for (Message message : messages) {
            if (message == null) continue;
            String text = message.getTextContent();
            if (text != null) {
                total += text.length() / 4L;
            }
        }
        return total;
    };

    @Resource
    private ModelService modelService;

    /**
     * 压缩状态存储（进程内内存实现，服务重启后摘要状态丢失）。
     */
    private final InMemoryAgentCompressionStateStore stateStore = new InMemoryAgentCompressionStateStore();

    /**
     * 为指定会话创建增量压缩策略。
     * 策略内部通过处理器自动管理状态加载、压缩执行和 CAS 保存，调用方无需手动保存。
     * <p>
     * 摘要模型固定使用 Bot 当前配置的对话模型；模型缺失或构建失败时记录日志并返回 null（跳过压缩，不影响对话）。
     * </p>
     *
     * @param aiBot          Bot 实体
     * @param conversationId 会话 ID（仅用于失败日志定位）
     * @return 增量压缩策略；模型不可用或创建失败时返回 null
     */
    public AgentContextCompressionPolicy createCompressionPolicy(Bot aiBot, String conversationId) {
        if (aiBot == null || aiBot.getModelId() == null) {
            return null;
        }
        if (stateStore == null) {
            log.warn("压缩状态存储未初始化，跳过压缩");
            return null;
        }
        try {
            // 摘要模型固定为当前对话模型
            Model compressionModelEntity = modelService.getModelInstance(aiBot.getModelId());
            if (compressionModelEntity == null) {
                log.warn("摘要模型未找到（使用当前对话模型）, modelId={}", aiBot.getModelId());
                return null;
            }
            ChatModel summaryChatModel = compressionModelEntity.toChatModel(null);
            if (summaryChatModel == null) {
                log.warn("摘要模型 ChatModel 构建失败, modelId={}", aiBot.getModelId());
                return null;
            }

            // 内置摘要指令
            String compressionInstruction = aiBot.getCompressionInstruction();
            // 创建模型摘要压缩器
            AgentContextCompressor delegate =
                    AgentContextCompressors.model(summaryChatModel, compressionInstruction);
            // 包装压缩器：把"摘要模型调用失败"和"摘要输出不合法"都写入日志后原样抛出，
            // 由策略层的 USE_ORIGINAL 降级为使用未压缩的原始历史（不打断本轮对话）。
            AgentContextCompressor compressor = messages -> {
                try {
                    List<Message> summary = delegate.compress(messages);
                    AgentContextCompressionValidator.validate(summary, true);
                    return summary;
                } catch (RuntimeException error) {
                    log.warn("摘要压缩失败，降级为使用原始历史：botId={}, conversationId={}, pendingMessages={}, error={}",
                            aiBot.getId(), conversationId, messages == null ? 0 : messages.size(), error.toString());
                    throw error;
                }
            };

            // 固定：待压缩历史达到 2 轮即触发摘要
            AgentContextCompressionDecider decider =
                    AgentContextCompressionDeciders.pendingTurnsAtLeast(aiBot.getCompressionTurnThreshold());

            return AgentContextCompressionPolicy.builder()
                    .stateStore(stateStore)
                    .decider(decider)
                    .compressor(compressor)
                    .tokenEstimator(TOKEN_ESTIMATOR)
                    // 固定：保留最近 2 轮原文
                    .keepRecentTurns(aiBot.getCompressionKeepRecentTurns())
                    .compactCompletedToolTurns(aiBot.isCompactCompletedToolTurns())
                    // 摘要失败时保留原始历史，避免整轮对话报错
                    .compressionFailureStrategy(AgentCompressionFailureStrategy.USE_ORIGINAL)
                    .build();
        } catch (Exception e) {
            log.error("创建压缩策略失败", e);
            return null;
        }
    }
}
