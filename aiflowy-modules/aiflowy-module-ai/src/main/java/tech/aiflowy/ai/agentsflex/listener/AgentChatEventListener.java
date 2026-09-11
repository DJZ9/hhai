package tech.aiflowy.ai.agentsflex.listener;

import com.agentsflex.agent.event.AgentEvent;
import com.agentsflex.agent.event.AgentEventListener;
import com.agentsflex.skill.runtime.SkillRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.core.chat.protocol.ChatDomain;
import tech.aiflowy.core.chat.protocol.ChatEnvelope;
import tech.aiflowy.core.chat.protocol.ChatType;
import tech.aiflowy.core.chat.protocol.MessageRole;
import tech.aiflowy.core.chat.protocol.payload.ErrorPayload;

import java.util.*;

/**
 * Agent 事件监听器：将 AgentsFlex Agent 运行时事件映射为现有前端 SSE 协议事件。
 * 替代原 ChatStreamListener 中的 SSE 事件发送逻辑。
 */
public class AgentChatEventListener implements AgentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentChatEventListener.class);

    private final String botId;
    private final String accountId;
    private final String conversationId;
    private final ChatSseEmitter sseEmitter;
    private final boolean exposeDeepThinking;

    private SkillRuntime skillRuntime;

    // 用于缓冲空行/空格 delta
    private final StringBuffer deltaMessageBuffer = new StringBuffer();

    private long startTime;
    private long requestTime = 0;

    public AgentChatEventListener(String botId, String accountId, String conversationId,
                                  ChatSseEmitter sseEmitter, boolean exposeDeepThinking) {
        this.botId = botId;
        this.accountId = accountId;
        this.conversationId = conversationId;
        this.sseEmitter = sseEmitter;
        this.exposeDeepThinking = exposeDeepThinking;
        this.startTime = System.currentTimeMillis();
    }

    public void setSkillRuntime(SkillRuntime skillRuntime) {
        this.skillRuntime = skillRuntime;
    }

    @Override
    public void onEvent(AgentEvent event) {
        try {
            switch (event.getType()) {
                case MODEL_TEXT_DELTA:
                    handleTextDelta(event);
                    break;
                case MODEL_REASONING_DELTA:
                    handleReasoningDelta(event);
                    break;
                case TURN_COMPLETED:
                    handleTurnCompleted();
                    break;
                case TURN_FAILED:
                    handleTurnFailed(event);
                    break;
                case MAX_ITERATIONS_REACHED:
                    handleMaxIterationsReached();
                    break;
                case MAX_STEPS_REACHED:
                    handleMaxStepsReached(event);
                    break;
                case BUDGET_EXCEEDED:
                    handleBudgetExceeded(event);
                    break;
                case CONTEXT_COMPRESSION_STARTED:
                    handleCompressionStarted(event);
                    break;
                case CONTEXT_COMPRESSION_COMPLETED:
                    handleCompressionCompleted(event);
                    break;
                case CONTEXT_COMPRESSION_FAILED:
                    handleCompressionFailed(event);
                    break;
                case TURN_CANCELLED:
                    handleTurnCancelled(event);
                    break;
                default:
                    // 未知事件类型记录日志，便于排查
                    log.debug("未处理的 Agent 事件类型: {}", event.getType());
                    break;
            }
        } catch (Exception e) {
            log.error("处理 Agent 事件异常: {}", event.getType(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleTextDelta(AgentEvent event) {
        Object content = event.getData().get("content");
        if (content == null) return;

        String delta = content.toString();
        if (delta == null) return;

        if (StringUtil.hasText(delta)) {
            try {
                sendChatEnvelope(deltaMessageBuffer + delta, ChatType.MESSAGE);
            } finally {
                deltaMessageBuffer.delete(0, deltaMessageBuffer.length());
            }
        } else {
            // 空行/空格内容暂存，等待有内容时一起输出
            deltaMessageBuffer.append(delta);
        }
    }

    private void handleReasoningDelta(AgentEvent event) {
        if (!exposeDeepThinking) return;

        Object content = event.getData().get("content");
        if (content == null) return;

        String reasoning = content.toString();
        if (StringUtils.hasText(reasoning)) {
            sendChatEnvelope(reasoning, ChatType.THINKING);
        }
    }

    private void handleTurnCompleted() {
        try {
            if (requestTime == 0) {
                requestTime = System.currentTimeMillis() - startTime;
            }

            // 发送 done 事件
            ChatEnvelope<Map<String, String>> doneEnvelope = new ChatEnvelope<>();
            doneEnvelope.setDomain(ChatDomain.SYSTEM);
            sseEmitter.sendDone(doneEnvelope);

        } catch (Exception e) {
            log.error("处理对话完成事件异常", e);
        } finally {
            closeSkillRuntime();
        }
    }

    private void handleTurnFailed(AgentEvent event) {
        Object error = event.getData().get("error");
        String message = error != null ? error.toString() : "对话处理失败";
        sendSystemError(message);
    }

    private void handleMaxIterationsReached() {
        sendSystemError("工具调用次数超出限制，请重新开始会话。");
    }

    /**
     * 处理步骤数达到上限事件
     * 从 event 数据中提取 steps 和 maxSteps，给出明确提示
     */
    private void handleMaxStepsReached(AgentEvent event) {
        Object steps = event.getData().get("steps");
        Object maxSteps = event.getData().get("maxSteps");
        String message;
        if (steps != null && maxSteps != null) {
            message = "执行步骤数达到上限（" + steps + "/" + maxSteps + "），请重新开始会话。";
        } else {
            message = "执行步骤数超出限制，请重新开始会话。";
        }
        sendSystemError(message);
    }

    /**
     * 处理预算耗尽事件（工具调用/Token/时长等）
     * 从 event 数据中提取 reason，给出明确提示
     */
    private void handleBudgetExceeded(AgentEvent event) {
        Object reason = event.getData().get("reason");
        String message;
        if (reason != null) {
            message = "执行预算已耗尽：" + reason + "，请重新开始会话。";
        } else {
            message = "执行预算已耗尽，请重新开始会话。";
        }
        sendSystemError(message);
    }

    /**
     * 处理 Turn 被取消事件
     * 从 event 数据中提取原因，给出明确提示
     */
    private void handleTurnCancelled(AgentEvent event) {
        Object reason = event.getData().get("reason");
        String message;
        if (reason != null) {
            message = "对话已被取消：" + reason;
        } else {
            message = "对话已被取消，请重新开始会话。";
        }
        sendSystemError(message);
    }

    /**
     * 处理上下文摘要压缩开始事件（仅记录日志，不产生 SSE 事件）。
     */
    private void handleCompressionStarted(AgentEvent event) {
        Map<String, Object> data = event.getData();
        log.info("开始摘要压缩: conversationId={}, historyMessageCount={}, pendingMessageCount={}",
                conversationId, data == null ? null : data.get("historyMessageCount"),
                data == null ? null : data.get("pendingMessageCount"));
    }

    /**
     * 处理上下文摘要压缩完成事件（仅记录日志，不产生 SSE 事件）。
     */
    private void handleCompressionCompleted(AgentEvent event) {
        Map<String, Object> data = event.getData();
        log.info("摘要压缩完成: conversationId={}, compressed={}, coveredUntilMessageId={}, "
                        + "stateVersion={}, modelMessageCount={}",
                conversationId, data == null ? null : data.get("compressed"),
                data == null ? null : data.get("coveredUntilMessageId"),
                data == null ? null : data.get("stateVersion"),
                data == null ? null : data.get("modelMessageCount"));
    }

    /**
     * 处理上下文摘要压缩失败事件（降级策略下通常不会触发，作为兜底记录 warn 日志）。
     */
    private void handleCompressionFailed(AgentEvent event) {
        Map<String, Object> data = event.getData();
        log.warn("摘要压缩失败: conversationId={}, error={}",
                conversationId, data == null ? null : data.get("error"));
    }

    // ==================== SSE 发送辅助方法 ====================

    private void sendChatEnvelope(String deltaContent, ChatType chatType) {
        if (deltaContent == null || deltaContent.isEmpty()) {
            return;
        }

        ChatEnvelope<Map<String, String>> chatEnvelope = new ChatEnvelope<>();
        chatEnvelope.setDomain(ChatDomain.LLM);
        chatEnvelope.setType(chatType);

        Map<String, String> deltaMap = new LinkedHashMap<>();
        deltaMap.put("role", MessageRole.ASSISTANT.getValue());
        deltaMap.put("delta", deltaContent);
        chatEnvelope.setConversationId(this.conversationId);
        chatEnvelope.setPayload(deltaMap);
        sseEmitter.send(chatEnvelope);
    }

    public void sendSystemError(String message) {
        ChatEnvelope<ErrorPayload> envelope = new ChatEnvelope<>();
        ErrorPayload payload = new ErrorPayload();
        payload.setMessage(message);
        payload.setCode("SYSTEM_ERROR");
        payload.setRetryable(false);
        envelope.setPayload(payload);
        envelope.setDomain(ChatDomain.SYSTEM);
        envelope.setType(ChatType.ERROR);
        sseEmitter.sendError(envelope);
        sseEmitter.complete();
    }

    /**
     * 关闭 SkillRuntime 释放资源
     */
    private void closeSkillRuntime() {
        try {
            if (skillRuntime != null) {
                skillRuntime.close();
                log.info("关闭 SkillRuntime 成功");
            }
        } catch (Exception e) {
            log.error("关闭 SkillRuntime 时发生异常", e);
        }
    }
}
