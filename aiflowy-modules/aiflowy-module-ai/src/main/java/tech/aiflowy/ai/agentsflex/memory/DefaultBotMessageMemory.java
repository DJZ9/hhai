package tech.aiflowy.ai.agentsflex.memory;

import com.agentsflex.core.memory.DefaultChatMemory;
import com.agentsflex.core.message.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.core.chat.protocol.ChatDomain;
import tech.aiflowy.core.chat.protocol.ChatEnvelope;
import tech.aiflowy.core.chat.protocol.ChatType;
import tech.aiflowy.core.chat.protocol.MessageRole;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class DefaultBotMessageMemory extends DefaultChatMemory {

    private final ChatSseEmitter sseEmitter;

    private final List<Map<String, String>> messages;
    public DefaultBotMessageMemory(BigInteger conversationId, ChatSseEmitter sseEmitter, List<Map<String, String>> messages) {
        super(conversationId);
        this.sseEmitter = sseEmitter;
        this.messages = messages;
    }

    @Override
    public List<Message> getMessages(int count) {
        List<Message> list = new ArrayList<>(messages.size());
        for (Map<String, String> msg : messages) {
            BotMessage botMessage = new BotMessage();
            botMessage.setRole(msg.get("role"));
            botMessage.setContent(msg.get("content"));
            Message message = botMessage.getContentAsMessage();
            list.add(message);
        }
        List<Message> collect = list.stream()
                .limit(count)
                .collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<Message> getModelMessages(int count) {
        return getMessages(count);
    }

    @Override
    public void addMessage(Message message) {
        BotMessage dbMessage = new BotMessage();
        ChatEnvelope<Map<String, String>> chatEnvelope = new ChatEnvelope<>();
        String jsonMessage = JSON.toJSONString(message, SerializerFeature.WriteClassName);
        if (message instanceof AiMessage) {
            dbMessage.setRole(MessageRole.ASSISTANT.getValue());

        } else if (message instanceof UserMessage) {
            dbMessage.setRole(MessageRole.USER.getValue());
        } else if (message instanceof SystemMessage) {
            dbMessage.setRole(MessageRole.SYSTEM.getValue());
        } else if (message instanceof ToolMessage) {
            dbMessage.setRole(MessageRole.TOOL.getValue());
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

}
