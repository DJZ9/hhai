package tech.aiflowy.core.chat.protocol.sse;

import com.alibaba.fastjson.JSON;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.core.chat.protocol.ChatEnvelope;

import java.io.IOException;
import java.time.Duration;

public class ChatSseEmitter {

    private static final long DEFAULT_TIMEOUT = Duration.ofMinutes(5).toMillis();

    private final SseEmitter emitter;

    public ChatSseEmitter() {
        this(DEFAULT_TIMEOUT);
    }

    public ChatSseEmitter(long timeoutMillis) {
        this.emitter = new SseEmitter(timeoutMillis);
    }

    public SseEmitter getEmitter() {
        return emitter;
    }

    /** 发送普通 ChatEnvelope（event: message） */
    public void send(ChatEnvelope<?> envelope) {
        send("message", envelope);
    }

    /** 发送 error 事件 */
    public void sendError(ChatEnvelope<?> envelope) {
        send("error", envelope);
    }

    /** 发送 done 事件并关闭 */
    public void sendDone(ChatEnvelope<?> envelope) {
        send("done", envelope);
        complete();
    }

    /** 🔥 新增：发送并立即关闭 */
    public void sendAndClose(ChatEnvelope<?> envelope) {
        send("message", envelope);
        ThreadPoolTaskExecutor threadPoolTaskExecutor = SpringContextUtil.getBean("sseThreadPool");
        threadPoolTaskExecutor.execute(() -> {
            try {
                Thread.sleep(500);
                complete();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** 通知前端保存该消息 */
    public void sendMessageNeedSave(ChatEnvelope<?> envelope) {
        send("needSaveMessage", envelope);
    }

    /** SSE 底层发送 */
    private void send(String event, ChatEnvelope<?> envelope) {
        try {
            String json = JSON.toJSONString(envelope);
            emitter.send(
                    SseEmitter.event()
                            .name(event)
                            .data(json)
            );
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * 发送消息ID给前端（用于关联后端保存的消息）
     */
    public void sendMessageId(ChatEnvelope<?> envelope) {
        send("messageId", envelope);
    }

    public void complete() {
        emitter.complete();
    }

    public void completeWithError(Throwable ex) {
        emitter.completeWithError(ex);
    }
}
