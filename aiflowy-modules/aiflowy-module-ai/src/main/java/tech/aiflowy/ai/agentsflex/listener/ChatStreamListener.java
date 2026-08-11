package tech.aiflowy.ai.agentsflex.listener;

import cn.hutool.core.util.IdUtil;
import com.agentsflex.core.audio.tts.StreamingTextToSpeechModel;
import com.agentsflex.core.document.Document;
import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.ToolCall;
import com.agentsflex.core.message.ToolMessage;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.chat.tool.Tool;
import com.agentsflex.core.model.chat.tool.ToolExecutor;
import com.agentsflex.core.model.client.StreamContext;
import com.agentsflex.core.prompt.MemoryPrompt;
import com.agentsflex.core.util.CollectionUtil;
import com.agentsflex.skill.runtime.SkillRuntime;
import com.alibaba.fastjson2.JSON;
import com.alicp.jetcache.Cache;
import dev.tinyflow.core.chain.ChainSuspendException;
import dev.tinyflow.core.chain.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import tech.aiflowy.ai.agentsflex.message.FormMessage;
import tech.aiflowy.ai.agentsflex.tool.DocumentCollectionTool;
import tech.aiflowy.ai.entity.Bot;
import tech.aiflowy.ai.entity.BotToolCallRecord;
import tech.aiflowy.ai.service.BotService;
import tech.aiflowy.ai.service.BotToolCallRecordService;
import tech.aiflowy.ai.service.ConversationCompressionService;
import tech.aiflowy.ai.utils.EventUtil;
import tech.aiflowy.common.audio.listener.AudioMessageListener;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.core.chat.protocol.ChatDomain;
import tech.aiflowy.core.chat.protocol.ChatEnvelope;
import tech.aiflowy.core.chat.protocol.ChatType;
import tech.aiflowy.core.chat.protocol.MessageRole;
import tech.aiflowy.core.chat.protocol.payload.ErrorPayload;
import tech.aiflowy.core.chat.protocol.payload.FormData;
import tech.aiflowy.core.chat.protocol.payload.FormInfo;
import tech.aiflowy.core.chat.protocol.payload.FormRequestPayload;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// REMOVE_TAG_START:COMMERCIAL
// REMOVE_TAG_END:COMMERCIAL

public class ChatStreamListener implements StreamResponseListener {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamListener.class);

    private final String botId;
    private final String accountId;
    private final String conversationId;
    private final ChatModel chatModel;
    private final MemoryPrompt memoryPrompt;
    private final ChatSseEmitter sseEmitter;
    private final ChatOptions chatOptions;
    // 核心标记：是否允许执行onStop业务逻辑（仅最后一次无后续工具调用时为true）
    private boolean canStop = true;
    // 辅助标记：是否进入过工具调用（避免重复递归判断）
    private boolean hasToolCall = false;
    private long startTime;
    private long requestTime = 0;
    private long chatTime;
    // 是否显示思考过程
    private final boolean exposeDeepThinking;
    // 工具最大调用次数限制
    private final AtomicInteger maxToolCallCount = new AtomicInteger(0);
    // 当前登录用户信息
    private final LoginAccount loginAccount;

    private final StringBuffer deltaMessageBuffer = new StringBuffer();

    // 收集知识库引用的列表
    private final List<Map<String, Object>> knowledgeReferences = new ArrayList<>();

    private StreamingTextToSpeechModel streamTTSModel;

    private final AudioMessageListener audioMessageListener = new AudioMessageListener() {
        @Override
        public void onMessageReceived(String sessionId, String messageId, byte[] message) {
//            sendChatEnvelope();
        }

        @Override
        public void onFinished(String sessionId, String messageId, byte[] fullMessage) {

        }

        @Override
        public void onError(String sessionId, String messageId, Exception e) {

        }
    };

//    public ChatStreamListener(String conversationId, ChatModel chatModel, MemoryPrompt memoryPrompt, ChatSseEmitter sseEmitter, ChatOptions chatOptions) {
//        this.conversationId = conversationId;
//        this.chatModel = chatModel;
//        this.memoryPrompt = memoryPrompt;
//        this.sseEmitter = sseEmitter;
//        this.chatOptions = chatOptions;
//    }


    public ChatStreamListener(String botId, String accountId, String conversationId, ChatModel chatModel, MemoryPrompt memoryPrompt, ChatSseEmitter sseEmitter,
                              ChatOptions chatOptions, boolean exposeDeepThinking, LoginAccount loginAccount) {
        this.botId = botId;
        this.accountId = accountId;
        this.conversationId = conversationId;
        this.chatModel = chatModel;
        this.memoryPrompt = memoryPrompt;
        this.sseEmitter = sseEmitter;
        this.chatOptions = chatOptions;
        this.exposeDeepThinking = exposeDeepThinking;
        this.loginAccount = loginAccount;
    }

    @Override
    public void onOpen(StreamContext context) {
        this.startTime = System.currentTimeMillis();
        StreamResponseListener.super.onOpen(context);
    }

    @Override
    public void onMessage(StreamContext context, AiMessageResponse aiMessageResponse) {
        try {
            if (maxToolCallCount.get() >= 60) {
                sendSystemError(sseEmitter, "工具调用次数超出限制，请重新开始会话。");
                return;
            }

            // 如果请求处理大于 5 分钟，则结束会话，避免长时间等待以及死循环保护
            if (System.currentTimeMillis() - startTime > 5 * 60 * 1000) {
                sendSystemError(sseEmitter, "请求处理超时，请重新开始会话。");
                return;
            }

            AiMessage aiMessage = aiMessageResponse.getMessage();
            if (requestTime == 0) {
                requestTime = System.currentTimeMillis() - startTime;
            }

            if (aiMessage == null) {
                return;
            }
            boolean isSuspended = false;
            if (aiMessage.isFinalDelta() && aiMessageResponse.hasToolCalls()) {
                this.canStop = false; // 工具调用期间，禁止执行onStop
                this.hasToolCall = true; // 标记已进入过工具调用
                memoryPrompt.addMessage(aiMessage);
                List<ToolCall> toolCalls = aiMessage.getToolCalls();
                for (ToolCall toolCall : toolCalls) {
                    Map<String, String> toolCallMap = new HashMap<>();
                    if (!StringUtils.hasText(toolCall.getId())) {
                        continue;
                    }
                    if (StringUtils.hasText(toolCall.getName())) {
                        toolCallMap.put("name", toolCall.getName());
                    }
                    if (StringUtils.hasText(toolCall.getId())) {
                        toolCallMap.put("tool_call_id", toolCall.getId());
                    }
                    if (toolCall.getArgsMap() != null) {
                        Map<String, Object> map = new HashMap<>();
                        try {
                            map = toolCall.getArgsMap();
                        } catch (Exception e) {
                            log.error("toolCall.getArgsMap() to json error", e);
                        }
                        toolCallMap.put("arguments", map.toString());
                    }
                    sendChatEnvelope(ChatDomain.TOOL, ChatType.TOOL_CALL, toolCallMap);
                }
//                List<ToolMessage> toolMessages = aiMessageResponse.executeToolCallsAndGetToolMessages();

                List<ToolExecutor> toolExecutors = aiMessageResponse.getToolExecutors();
                if (!CollectionUtil.noItems(toolExecutors)) {

                    for(ToolExecutor toolExecutor : toolExecutors) {
                        ToolMessage toolMessage = new ToolMessage();
                        String callId = toolExecutor.getToolCall().getId();
                        if (com.agentsflex.core.util.StringUtil.hasText(callId)) {
                            toolMessage.setToolCallId(callId);
                        } else {
                            toolMessage.setToolCallId(toolExecutor.getToolCall().getName());
                        }

                        Object result = null;
                        String stateId = null;
                        List<Parameter> suspendParameters = null;
                        try {
                            result = toolExecutor.execute();
                        } catch (ChainSuspendException e) {
                            // REMOVE_TAG_START:COMMERCIAL
                            isSuspended = true;
                            stateId = e.getChainStateId();
                            suspendParameters = e.getSuspendParameters();
                            // REMOVE_TAG_END:COMMERCIAL
                        }

                        // 提取知识库引用信息
                        extractKnowledgeReferences(result, toolExecutor);

                        if (result != null && !(result instanceof CharSequence) && !(result instanceof Number)) {
                            toolMessage.setContent(JSON.toJSONString(result));
                        } else if (result != null){
                            toolMessage.setContent(result.toString());
                        }

                        // REMOVE_TAG_START:COMMERCIAL
                        if (isSuspended && CollectionUtil.hasItems(suspendParameters)) {

                            @SuppressWarnings("unchecked")
                            Cache<String, Object> defaultCache = SpringContextUtil.getBean("defaultCache", Cache.class);

                            String formId = IdUtil.fastSimpleUUID();
                            FormInfo formInfo = new FormInfo();
                            formInfo.setFormId(formId);
                            formInfo.setToolCallId(toolMessage.getToolCallId());
                            formInfo.setBotId(new BigInteger(botId));
                            formInfo.setConversationId(new BigInteger(conversationId));
                            formInfo.setChainStateId(stateId);


                            FormRequestPayload payload = new FormRequestPayload();
                            payload.setFormId(formId);
                            List<FormData> formDataList = new ArrayList<>();
                            for (Parameter p : suspendParameters) {
                                FormData formData = new FormData();
                                formData.setAttrName(p.getName());
                                formData.setType(p.getFormType());
                                formData.setContentType(p.getContentType());
                                formData.setLabel(p.getFormLabel());
                                formData.setOptions(p.getEnums());
                                formData.setRequired(p.isRequired());
                                formDataList.add(formData);
                            }
                            payload.setFormData(formDataList);

                            FormMessage formMessage = new FormMessage();
                            formMessage.setContent(JSON.toJSONString(payload));
                            memoryPrompt.addMessage(formMessage);
                            // 设置 messageId，便于执行工作流时更新表单内容
                            formInfo.setMessageId(formMessage.getMessageId());
                            defaultCache.put(formId, formInfo);

                            ChatEnvelope<FormRequestPayload> chatEnvelope = new ChatEnvelope<>();
                            chatEnvelope.setDomain(ChatDomain.INTERACTION);
                            chatEnvelope.setType(ChatType.FORM_REQUEST);
                            chatEnvelope.setPayload(payload);
                            sseEmitter.send(chatEnvelope);
                            this.canStop = true;
                        }
                        // REMOVE_TAG_END:COMMERCIAL

                        if (!isSuspended) {
                            memoryPrompt.addMessage(toolMessage);
                            Map<String, String> toolCallMap = new HashMap<>();
                            toolCallMap.put("tool_call_id", toolMessage.getToolCallId());
                            toolCallMap.put("result", toolMessage.getContent());
                            sendChatEnvelope(ChatDomain.TOOL, ChatType.TOOL_RESULT, toolCallMap);
                            saveToolCallRecord(toolMessage, toolExecutor);
                        }
                    }
                }

                // 工具调用次数增加
                if (!isSuspended) {
                    maxToolCallCount.incrementAndGet();
                    chatModel.chatStream(memoryPrompt, this, chatOptions);
                }
            } else {
                if (this.hasToolCall) {
                    this.canStop = true;
                }
                String reasoningContent = aiMessage.getReasoningContent();
                if (StringUtils.hasText(reasoningContent) && exposeDeepThinking) {
                    sendChatEnvelope(reasoningContent, ChatType.THINKING);
                } else {
                    if ("".equals(aiMessage.getContent())
                            && "".equals(aiMessage.getFullContent())
                            && StringUtil.noText(aiMessage.getFullReasoningContent())) {
                        return;  // ignore 一般情况第一条消息
                    }
                    if ("\n\n".equals(aiMessage.getContent())
                            && "\n\n".equals(aiMessage.getFullContent())
                            && StringUtil.hasText(aiMessage.getFullReasoningContent())) {
                        return; // ignore 一般情况下是 reasoning 和 tool_call 间隔消息，忽略
                    }

                    String delta = aiMessage.getContent();
                    if (delta == null) {
                        return;
                    }

                    if (StringUtil.hasText(delta)) {
                        try {
                            if (aiMessage.getFinished() != null && aiMessage.getFinished()) {
                                return;
                            }
                            sendChatEnvelope(deltaMessageBuffer + delta, ChatType.MESSAGE);

//                            streamTTSModel.sendText("xxxx");


                        } finally {
                            maxToolCallCount.set(0);
                            deltaMessageBuffer.delete(0, deltaMessageBuffer.length());
                        }
                    } else {
                        // 可能是换行、空格等内容，不对外输出，暂时缓存到 deltaMessageBuffer，等待有内容时一起对外输出。
                        deltaMessageBuffer.append(delta);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClose(StreamContext context) {
        if (this.canStop) {
            try {
                if (context.getThrowable() != null) {
                    sendSystemError(sseEmitter, context.getThrowable().getMessage());
                    return;
                }
                memoryPrompt.addMessage(context.getFullMessage());

                BigInteger lastAiMessageId = getLastAiMessageIdFromMemory();

                if (lastAiMessageId != null) {
                    ChatEnvelope<Map<String, Object>> messageIdEnvelope = new ChatEnvelope<>();
                    messageIdEnvelope.setDomain(ChatDomain.SYSTEM);
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("messageId", lastAiMessageId.toString());
                    messageIdEnvelope.setPayload(payload);
                    sseEmitter.sendMessageId(messageIdEnvelope);
                }

                // 发送知识库引用事件
                sendKnowledgeReferences();

                ChatEnvelope<Map<String, String>> chatEnvelope = new ChatEnvelope<>();
                chatEnvelope.setDomain(ChatDomain.SYSTEM);
                sseEmitter.sendDone(chatEnvelope);
                StreamResponseListener.super.onClose(context);

                if (chatTime == 0) {
                    chatTime = System.currentTimeMillis() - startTime;
                }

                // REMOVE_TAG_START:COMMERCIAL
                EventUtil.publishStreamChatEvent(context, chatTime, requestTime);
                // REMOVE_TAG_END:COMMERCIAL

                try {
                    ConversationCompressionService compressionService = SpringContextUtil.getBean(ConversationCompressionService.class);
                    if (botId != null && accountId != null && conversationId != null) {
                        compressionService.compressConversationHistory(
                                new BigInteger(botId),
                                new BigInteger(accountId),
                                new BigInteger(conversationId)
                        );
                    }
                } catch (Exception e) {
                    log.error("压缩对话历史失败: {}", e.getMessage(), e);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // 关闭 SkillRuntime 释放资源
                closeSkillRuntime();
            }


        }


    }

    /**
     * 关闭 SkillRuntime 释放资源
     */
    private void closeSkillRuntime() {
        try {
            if (chatOptions != null && chatOptions.getContextAttributes() != null) {
                Object runtimeObj = chatOptions.getContextAttributes().get("skillRuntime");
                if (runtimeObj instanceof SkillRuntime runtime) {
                    runtime.close();
                    log.info("关闭 SkillRuntime 成功");
                }
            }
        } catch (Exception e) {
            log.error("关闭 SkillRuntime 时发生异常", e);
        }
    }

    /**
     * 从Memory中获取最后一条AI消息的ID
     */
    private BigInteger getLastAiMessageIdFromMemory() {
        try {
            // 获取Memory对象
            Object memory = memoryPrompt.getMemory();
            if (memory instanceof tech.aiflowy.ai.agentsflex.memory.BotMessageMemory) {
                tech.aiflowy.ai.agentsflex.memory.BotMessageMemory botMessageMemory =
                        (tech.aiflowy.ai.agentsflex.memory.BotMessageMemory) memory;
                return botMessageMemory.getLastAiMessageId();
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }

    @Override
    public void onError(StreamContext context, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
            sendSystemError(sseEmitter, throwable.getMessage());
        }
    }

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

    public void sendChatEnvelope(ChatDomain domain,
                                 ChatType type,
                                 Map<String, String> customParamMap) {
        ChatEnvelope<Map<String, String>> chatEnvelope = new ChatEnvelope<>();
        chatEnvelope.setDomain(domain);
        chatEnvelope.setType(type);
        chatEnvelope.setPayload(customParamMap == null ? new HashMap<>() : customParamMap);
        sseEmitter.send(chatEnvelope);
    }

    /**
     * 发送系统错误并且关闭会话
     *
     * @param sseEmitter
     * @param message
     */
    public void sendSystemError(ChatSseEmitter sseEmitter,
                                String message) {
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
     * 从工具执行结果中提取知识库引用信息
     * 相同path的分段content会合并到一起
     *
     * @param result 工具执行结果（List<Document>）
     * @param toolExecutor 工具执行器
     */
    @SuppressWarnings("unchecked")
    private void extractKnowledgeReferences(Object result, ToolExecutor toolExecutor) {
        if (result == null) {
            return;
        }
        Tool tool = toolExecutor.getTool();
        // 检查是否是知识库工具（DocumentCollectionTool）
        if (!(tool instanceof DocumentCollectionTool)) {
            return;
        }

        try {
            // 直接处理 List<Document>
            List<Document> documents = null;

            if (result instanceof List) {
                documents = (List<Document>) result;
            }

            if (documents != null && !documents.isEmpty()) {
                // 使用Map来合并相同path的分段
                Map<String, Map<String, Object>> pathReferenceMap = new HashMap<>();
                // 记录每个path的分段计数
                Map<String, Integer> pathSegmentCount = new HashMap<>();

                for (Document document : documents) {
                    // 从 metadata 中获取来源信息
                    Map<String, Object> metadata = document.getMetadataMap();
                    if (metadata != null) {
                        String docId = (String) metadata.get("sourceDocumentId");
                        String docTitle = (String) metadata.get("sourceDocumentTitle");
                        String docPath = (String) metadata.get("sourceDocumentPath");
                        String content = document.getContent() != null ? document.getContent() : "";

                        if (docId == null || docPath == null) {
                            continue;
                        }

                        // 使用path作为key进行合并
                        if (pathReferenceMap.containsKey(docPath)) {
                            // 已存在相同path，追加content
                            Map<String, Object> existingRef = pathReferenceMap.get(docPath);
                            String existingContent = (String) existingRef.get("content");
                            StringBuilder sb = new StringBuilder(existingContent);

                            // 增加分段计数
                            int segmentNum = pathSegmentCount.getOrDefault(docPath, 1) + 1;
                            pathSegmentCount.put(docPath, segmentNum);

                            if (StringUtil.hasText(content)) {
                                if (sb.length() > 0) {
                                    sb.append("\n\n");
                                    sb.append("═").append("═".repeat(40)).append("\n");
                                    sb.append("  【分段 ").append(segmentNum).append("】\n");
                                    sb.append("═").append("═".repeat(40)).append("\n\n");
                                }
                                sb.append(content);
                            }
                            existingRef.put("content", sb.toString());
                        } else {
                            // 新的path，创建新的引用
                            Map<String, Object> reference = new HashMap<>();
                            reference.put("path", docPath);
                            reference.put("type", "document_collection");
                            reference.put("title", docTitle != null ? docTitle : "未知文档");

                            // 第一个分段添加标识
                            StringBuilder sb = new StringBuilder();
                            if (StringUtil.hasText(content)) {
                                sb.append("═").append("═".repeat(40)).append("\n");
                                sb.append("  【分段 1】\n");
                                sb.append("═").append("═".repeat(40)).append("\n\n");
                                sb.append(content);
                            }
                            reference.put("content", sb.toString());
                            reference.put("documentId", docId);
                            pathReferenceMap.put(docPath, reference);
                            pathSegmentCount.put(docPath, 1);
                        }
                    }
                }

                // 将合并后的引用添加到knowledgeReferences
                // 去重：检查是否已存在相同的文档引用（基于documentId）
                for (Map<String, Object> reference : pathReferenceMap.values()) {
                    String docId = (String) reference.get("documentId");
                    boolean exists = knowledgeReferences.stream()
                            .anyMatch(ref -> docId != null && docId.equals(ref.get("documentId")));

                    if (!exists) {
                        knowledgeReferences.add(reference);
                        System.out.println("Added knowledge reference: " + reference);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取知识库引用信息失败: {}", e.getMessage());
        }
    }

    /**
     * 发送知识库引用事件
     */
    private void sendKnowledgeReferences() {
        if (knowledgeReferences.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("referenceList", knowledgeReferences);

            ChatEnvelope<Map<String, Object>> referenceEnvelope = new ChatEnvelope<>();
            referenceEnvelope.setDomain(ChatDomain.LLM);
            referenceEnvelope.setType(ChatType.REFERENCES);
            referenceEnvelope.setPayload(payload);
            referenceEnvelope.setConversationId(this.conversationId);

            sseEmitter.send(referenceEnvelope);
            log.debug("发送知识库引用事件，共 {} 条引用", knowledgeReferences.size());
        } catch (Exception e) {
            log.error("发送知识库引用事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存工具调用记录到数据库
     */
    private void saveToolCallRecord(ToolMessage toolMessage, ToolExecutor toolExecutor) {
        try {
            BotToolCallRecordService recordService = SpringContextUtil.getBean(BotToolCallRecordService.class);
            BotToolCallRecord record = new BotToolCallRecord();
            record.setBotId(new BigInteger(botId));
            record.setConversationId(new BigInteger(conversationId));
            record.setToolName(toolExecutor.getToolCall().getName());
            record.setInputParams(JSON.toJSONString(toolExecutor.getToolCall().getArgsMap()));
            record.setOutputParams(toolMessage.getContent());
            record.setCreated(new Date());
            if (loginAccount != null && loginAccount.getId() != null) {
                record.setCreatedBy(loginAccount.getId());
            }

            // 获取智能体名称
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


}
