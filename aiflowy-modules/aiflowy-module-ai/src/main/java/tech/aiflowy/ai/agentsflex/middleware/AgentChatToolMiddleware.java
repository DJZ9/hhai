package tech.aiflowy.ai.agentsflex.middleware;

import com.agentsflex.agent.middleware.AgentMiddleware;
import com.agentsflex.agent.middleware.AgentMiddlewareContext;
import com.agentsflex.agent.middleware.AgentToolCallChain;
import com.agentsflex.agent.tool.AgentToolContext;
import com.agentsflex.core.message.ToolCall;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import tech.aiflowy.ai.entity.Bot;
import tech.aiflowy.ai.entity.BotToolCallRecord;
import tech.aiflowy.ai.service.BotService;
import tech.aiflowy.ai.service.BotToolCallRecordService;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.core.chat.protocol.ChatDomain;
import tech.aiflowy.core.chat.protocol.ChatEnvelope;
import tech.aiflowy.core.chat.protocol.ChatType;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent 工具调用中间件：在 aroundToolCall 中捕获工具执行结果，
 * 发送 TOOL_CALL/TOOL_RESULT SSE 事件并保存工具调用记录。
 */
public class AgentChatToolMiddleware implements AgentMiddleware {

    private static final Logger log = LoggerFactory.getLogger(AgentChatToolMiddleware.class);

    private final String botId;
    private final String accountId;
    private final String conversationId;
    private final ChatSseEmitter sseEmitter;
    private final LoginAccount loginAccount;

    public AgentChatToolMiddleware(String botId, String accountId, String conversationId,
                                   ChatSseEmitter sseEmitter, LoginAccount loginAccount) {
        this.botId = botId;
        this.accountId = accountId;
        this.conversationId = conversationId;
        this.sseEmitter = sseEmitter;
        this.loginAccount = loginAccount;
    }

    @Override
    public Object aroundToolCall(AgentMiddlewareContext context, AgentToolCallChain chain) {
        AgentToolContext toolContext = context.getToolContext();
        if (toolContext == null) {
            return chain.proceed(context);
        }

        ToolCall toolCall = toolContext.getToolCall();
        String toolCallId = toolContext.getToolCallId();
        String toolName = toolContext.getToolName();

        // 发送 TOOL_CALL SSE 事件
        sendToolCallEvent(toolName, toolCallId, toolCall);

        Object result = chain.proceed(context);

        // 发送 TOOL_RESULT SSE 事件
        String resultContent = formatResult(result);
        sendToolResultEvent(toolCallId, resultContent);

        // 保存工具调用记录
        saveToolCallRecord(toolName, toolCall, resultContent);

        return result;
    }

    // ==================== SSE 事件发送 ====================

    private void sendToolCallEvent(String toolName, String toolCallId, ToolCall toolCall) {
        try {
            Map<String, String> toolCallMap = new HashMap<>();
            if (StringUtils.hasText(toolName)) {
                toolCallMap.put("name", toolName);
            }
            if (StringUtils.hasText(toolCallId)) {
                toolCallMap.put("tool_call_id", toolCallId);
            }
            if (toolCall != null && toolCall.getArgsMap() != null) {
                Map<String, Object> map = new HashMap<>();
                try {
                    map = toolCall.getArgsMap();
                } catch (Exception e) {
                    log.error("toolCall.getArgsMap() to json error", e);
                }
                toolCallMap.put("arguments", map.toString());
            }
            sendChatEnvelope(ChatDomain.TOOL, ChatType.TOOL_CALL, toolCallMap);
        } catch (Exception e) {
            log.error("发送 TOOL_CALL 事件异常", e);
        }
    }

    private void sendToolResultEvent(String toolCallId, String resultContent) {
        try {
            Map<String, String> toolCallMap = new HashMap<>();
            toolCallMap.put("tool_call_id", toolCallId);
            toolCallMap.put("result", resultContent);
            sendChatEnvelope(ChatDomain.TOOL, ChatType.TOOL_RESULT, toolCallMap);
        } catch (Exception e) {
            log.error("发送 TOOL_RESULT 事件异常", e);
        }
    }

    private void sendChatEnvelope(ChatDomain domain, ChatType type, Map<String, String> customParamMap) {
        ChatEnvelope<Map<String, String>> chatEnvelope = new ChatEnvelope<>();
        chatEnvelope.setDomain(domain);
        chatEnvelope.setType(type);
        chatEnvelope.setPayload(customParamMap == null ? new HashMap<>() : customParamMap);
        sseEmitter.send(chatEnvelope);
    }

    // ==================== 工具调用记录保存 ====================

    private void saveToolCallRecord(String toolName, ToolCall toolCall, String resultContent) {
        try {
            BotToolCallRecordService recordService = SpringContextUtil.getBean(BotToolCallRecordService.class);
            BotToolCallRecord record = new BotToolCallRecord();
            record.setBotId(new BigInteger(botId));
            record.setConversationId(new BigInteger(conversationId));
            record.setToolName(toolName);
            if (toolCall != null && toolCall.getArgsMap() != null) {
                record.setInputParams(JSON.toJSONString(toolCall.getArgsMap()));
            }
            record.setOutputParams(resultContent);
            record.setCreated(new Date());
            if (loginAccount != null && loginAccount.getId() != null) {
                record.setCreatedBy(loginAccount.getId());
            }

            try {
                BotService botService = SpringContextUtil.getBean(BotService.class);
                Bot bot = botService.getById(new BigInteger(botId));
                if (bot != null) {
                    record.setBotTitle(bot.getTitle());
                }
            } catch (Exception e) {
                log.warn("获取智能体名称失败: {}", e.getMessage());
            }

            recordService.save(record);
        } catch (Exception e) {
            log.error("保存工具调用记录失败: {}", e.getMessage(), e);
        }
    }

    // ==================== 辅助方法 ====================

    private String formatResult(Object result) {
        if (result == null) return "";
        if (result instanceof CharSequence || result instanceof Number) {
            return result.toString();
        }
        return JSON.toJSONString(result);
    }
}
