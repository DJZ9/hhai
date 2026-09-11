package tech.aiflowy.ai.agentsflex.memory;

import com.agentsflex.core.memory.DefaultChatMemory;
import com.agentsflex.core.message.*;
import com.agentsflex.core.util.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.core.chat.protocol.ChatDomain;
import tech.aiflowy.core.chat.protocol.ChatEnvelope;
import tech.aiflowy.core.chat.protocol.ChatType;
import tech.aiflowy.core.chat.protocol.MessageRole;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class DefaultBotMessageMemory extends DefaultChatMemory {

    private final ChatSseEmitter sseEmitter;

    private final List<Map<String, String>> messages;
    private final AtomicInteger toolCallCount = new AtomicInteger(0);
    private final Set<String> savedMessageIds = ConcurrentHashMap.newKeySet();

    public DefaultBotMessageMemory(BigInteger conversationId, ChatSseEmitter sseEmitter, List<Map<String, String>> messages) {
        super(conversationId);
        this.sseEmitter = sseEmitter;
        this.messages = messages;
    }

    @Override
    public List<Message> getModelMessages(int count) {
        return getMessages(count);
    }

    @Override
    public List<Message> getMessages(int count) {
        List<Message> list = new ArrayList<>(messages.size());
        for (Map<String, String> msg : messages) {
            BotMessage botMessage = new BotMessage();
            botMessage.setRole(msg.get("role"));
            botMessage.setContent(msg.get("content"));
            Message message = convertToMessage(botMessage);
            if (message != null) {
                list.add(message);
            }
        }
        int totalSize = list.size();
        int countInt = toolCallCount.get() + count;
        int from = Math.max(0, totalSize - countInt);
        int to = totalSize;
        return list.subList(from, to);
    }

    private Message convertToMessage(BotMessage botMessage) {
        String content = botMessage.getContent();
        if (content != null && !content.isEmpty() && content.startsWith("{")) {
            try {
                return botMessage.getContentAsMessage();
            } catch (Exception ignored) {
                // 解析失败时降级为根据 role 创建对应 Message
            }
        }
        String role = botMessage.getRole();
        if (MessageRole.ASSISTANT.getValue().equals(role)) {
            return new AiMessage(content);
        } else if (MessageRole.USER.getValue().equals(role)) {
            return new UserMessage(content);
        } else if (MessageRole.SYSTEM.getValue().equals(role)) {
            return new SystemMessage(content);
        } else if (MessageRole.TOOL.getValue().equals(role)) {
            ToolMessage toolMessage = new ToolMessage();
            toolMessage.setContent(content);
            return toolMessage;
        }
        return new UserMessage(content);
    }

    @Override
    public void addMessage(Message message) {
        if (message instanceof ToolMessage || message instanceof AiMessage) {
            toolCallCount.incrementAndGet();
        }
        BotMessage dbMessage = new BotMessage();
        ChatEnvelope<Map<String, String>> chatEnvelope = new ChatEnvelope<>();
        String jsonMessage = JSON.toJSONString(message, SerializerFeature.WriteClassName);
        if (message instanceof AiMessage) {
            dbMessage.setRole(MessageRole.ASSISTANT.getValue());
        } else if (message instanceof SystemMessage) {
            dbMessage.setRole(MessageRole.SYSTEM.getValue());
        } else if (message instanceof ToolMessage) {
            dbMessage.setRole(MessageRole.TOOL.getValue());
        } else {
            dbMessage.setRole(MessageRole.USER.getValue());
        }
        Map<String, String> res = new HashMap<>();
        res.put("role", dbMessage.getRole());
        res.put("content", jsonMessage);
        chatEnvelope.setType(ChatType.MESSAGE);
        chatEnvelope.setPayload(res);
        chatEnvelope.setDomain(ChatDomain.SYSTEM);
        if (dbMessage.getRole().equals(MessageRole.USER.getValue())) {
            messages.remove(messages.size() - 1);
        }
        sseEmitter.sendMessageNeedSave(chatEnvelope);
        messages.add(res);
    }

    @Override
    public boolean addMessageIfAbsent(Message message) {
        if (message == null) {
            return false;
        }
        String messageId = message.getMessageId();
        if (messageId == null || messageId.isEmpty()) {
            addMessage(message);
            return true;
        }
        if (!savedMessageIds.add(messageId)) {
            return false;
        }
        addMessage(message);
        return true;
    }

}
