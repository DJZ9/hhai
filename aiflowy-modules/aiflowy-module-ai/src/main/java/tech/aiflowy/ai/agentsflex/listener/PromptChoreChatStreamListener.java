package tech.aiflowy.ai.agentsflex.listener;

import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.client.StreamContext;
import com.alibaba.fastjson.JSON;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.core.chat.protocol.ChatDomain;
import tech.aiflowy.core.chat.protocol.ChatEnvelope;
import tech.aiflowy.core.chat.protocol.ChatType;
import tech.aiflowy.core.chat.protocol.MessageRole;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统提示词优化监听器
 */
public class PromptChoreChatStreamListener implements StreamResponseListener {

    private final ChatSseEmitter sseEmitter;

    public PromptChoreChatStreamListener(ChatSseEmitter sseEmitter) {
        this.sseEmitter = sseEmitter;
    }
    @Override
    public void onStart(StreamContext context) {
        StreamResponseListener.super.onStart(context);
    }

    @Override
    public void onMessage(StreamContext context, AiMessageResponse response) {
        String content = response.getMessage().getContent();
        if (content != null) {
            String delta = response.getMessage().getContent();
            if (StringUtil.hasText(delta)) {
                ChatEnvelope<Map<String, String>> chatEnvelope = new ChatEnvelope<>();
                chatEnvelope.setDomain(ChatDomain.LLM);
                chatEnvelope.setType(ChatType.MESSAGE);
                Map<String, String> deletaMap = new HashMap<>();
                deletaMap.put("delta", delta);
                deletaMap.put("role", MessageRole.ASSISTANT.getValue());
                chatEnvelope.setPayload(deletaMap);
                sseEmitter.send(chatEnvelope);
            }
        }
    }

    @Override
    public void onStop(StreamContext context) {
        System.out.println("onStop");
        sseEmitter.sendDone(new ChatEnvelope<>());
        StreamResponseListener.super.onStop(context);
    }

    @Override
    public void onFailure(StreamContext context, Throwable throwable) {
        StreamResponseListener.super.onFailure(context, throwable);
    }
}
