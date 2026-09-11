package tech.aiflowy.ai.agentsflex.memory;

import com.agentsflex.core.memory.ChatMemory;
import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.Message;
import com.agentsflex.core.message.ToolCall;
import com.agentsflex.core.message.UserMessage;
import com.agentsflex.core.util.CollectionUtil;
import com.mybatisflex.core.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.ai.mapper.BotMessageMapper;
import tech.aiflowy.ai.service.BotConversationService;
import tech.aiflowy.ai.service.BotMessageService;
import tech.aiflowy.ai.service.userProfileService.DataScopeManager;

import static tech.aiflowy.ai.entity.table.BotMessageTableDef.BOT_MESSAGE;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executor;

public class BotMessageMemory implements ChatMemory {
    private static final Logger log = LoggerFactory.getLogger(BotMessageMemory.class);

    private final BigInteger botId;
    private final BigInteger accountId;
    private final BigInteger conversationId;
    private final BotMessageService messageService;
    private final BotMessageMapper messageMapper;
    private final BotConversationService conversationService;
    private final Executor commonExecutor;

    /**
     * count <= 0 时的防御性兜底条数。框架正常都会传入正数
     * （挂载消息上限，或增量压缩场景下的 Integer.MAX_VALUE），此处仅避免异常入参。
     */
    private static final int FALLBACK_MAX_MESSAGES = 5;

    /**
     * 本请求内已写入数据库的消息缓存，仅用于按 messageId 判重（不承载历史）。
     */
    private List<Message> messages = new ArrayList<>();

    private Integer currentRoundNumber = null;

    private String originalUserContent;

    public BotMessageMemory(BigInteger botId, BigInteger accountId, BigInteger conversationId,
                            BotMessageService messageService, BotMessageMapper messageMapper,
                            BotConversationService conversationService, Executor commonExecutor) {
        this.botId = botId;
        this.accountId = accountId;
        this.conversationId = conversationId;
        this.messageService = messageService;
        this.messageMapper = messageMapper;
        this.conversationService = conversationService;
        this.commonExecutor = commonExecutor;
    }

    @Override
    public List<Message> getMessages(int count) {
        int fetchCount = count > 0 ? count : FALLBACK_MAX_MESSAGES;

        // 从 DB 查询最近的 fetchCount 条消息（按 ID 降序取最新，再反转为时间正序）
        List<BotMessage> dbMessages = messageService.list(QueryWrapper.create()
                .eq(BotMessage::getBotId, botId, true)
                .eq(BotMessage::getAccountId, accountId, true)
                .eq(BotMessage::getConversationId, conversationId, true)
                .orderBy(BOT_MESSAGE.ID.desc())
                .limit(fetchCount)
        );

        if (!CollectionUtil.hasItems(dbMessages)) {
            return Collections.emptyList();
        }

        // 反转为时间正序
        Collections.reverse(dbMessages);

        // 转换为 Message 对象
        List<Message> result = new ArrayList<>();
        for (BotMessage dbMessage : dbMessages) {
            Message message = dbMessage.getContentAsMessage();
            if (message != null) {
                result.add(message);
            }
        }

        // 截断不完整的 tool_calls，确保序列合法
        return truncateIncompleteToolCalls(result);
    }

    /**
     * 分页读取历史（旧到新）。
     * <p>
     * 这是 ChatMemory 的接口方法：AgentContextWindow 在需要按页回读历史时会调用它。
     * 当前开源链路中 Agent 的上下文窗口读取的是 Turn 自带的内存 memory，因此本方法不会被触发；
     * 实现保留以保证接口兼容。
     * </p>
     */
    @Override
    public List<Message> getMessages(int offset, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        // 从 DB 查询：按 ID 降序（最新优先），取 offset+count 条，再手动跳过 offset 条
        int limit = offset + count;
        List<BotMessage> dbMessages = messageService.list(QueryWrapper.create()
                .eq(BotMessage::getBotId, botId, true)
                .eq(BotMessage::getAccountId, accountId, true)
                .eq(BotMessage::getConversationId, conversationId, true)
                .orderBy(BOT_MESSAGE.ID.desc())
                .limit(limit)
        );
        if (!CollectionUtil.hasItems(dbMessages)) {
            return Collections.emptyList();
        }
        // 跳过前 offset 条，反转为时间正序
        int startIdx = Math.min(offset, dbMessages.size());
        List<BotMessage> paged = new ArrayList<>(dbMessages.subList(startIdx, dbMessages.size()));
        Collections.reverse(paged);

        List<Message> result = new ArrayList<>();
        for (BotMessage msg : paged) {
            Message message = msg.getContentAsMessage();
            if (message != null) {
                result.add(message);
            }
        }
        return result;
    }

    @Override
    public List<Message> getModelMessages(int count) {
        // 复用 getMessages 逻辑，过滤仅用于 UI 展示、对模型不可见的消息
        List<Message> allMessages = getMessages(count);
        if (allMessages == null || allMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<Message> result = new ArrayList<>();
        for (Message message : allMessages) {
            if (message != null && message.isModelVisible()) {
                result.add(message);
            }
        }
        return result;
    }

    private Integer getMaxRoundNumber() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("MAX(round_number)")
                .eq(BotMessage::getBotId, botId)
                .eq(BotMessage::getAccountId, accountId)
                .eq(BotMessage::getConversationId, conversationId);

        Object result = messageMapper.selectObjectByQuery(queryWrapper);
        if (result instanceof Number) {
            return ((Number) result).intValue();
        }
        return null;
    }

    @Override
    public void addMessage(Message message) {
        if (message == null) {
            return;
        }
        if (message instanceof UserMessage) {
            // 用户消息：清空缓存，开始新的一轮对话
            this.messages.clear();
        }
        // 所有消息都添加到缓存（用户消息、工具调用消息、AI回复消息）
        this.messages.add(message);

        int roundNumber = getRoundNumberForMessage(message);

        BotMessage dbMessage = new BotMessage();
        dbMessage.setCreated(new Date());
        dbMessage.setBotId(botId);
        dbMessage.setAccountId(accountId);
        dbMessage.setConversationId(conversationId);
        // 用户消息保存原始 prompt，避免附件拼接内容混入历史记录
        if (message instanceof UserMessage && StringUtils.hasText(originalUserContent)) {
            dbMessage.setContentAndRole(new UserMessage(originalUserContent));
        } else {
            dbMessage.setContentAndRole(message);
        }
        dbMessage.setModified(new Date());
        dbMessage.setRoundNumber(roundNumber);

        // 存储 agents-flex messageId 和 version，用于 addMessageIfAbsent 和 getMessage 查询
        String afMessageId = message.getMessageId();
        if (afMessageId != null && !afMessageId.isEmpty()) {
            Map<String, Object> options = dbMessage.getOptions();
            if (options == null) {
                options = new HashMap<>();
            }
            options.put("af_messageId", afMessageId);
            options.put("af_version", message.getVersion());
            dbMessage.setOptions(options);
        }

        messageService.save(dbMessage);

        // 如果是用户第一次提问（roundNumber == 1），异步生成会话标题
        if (message instanceof UserMessage && roundNumber == 1) {
            String content = ((UserMessage) message).getContent();
            BigInteger currentAccountId = this.accountId;
            // 使用线程池异步执行，避免阻塞主线程
            if (commonExecutor != null) {
                commonExecutor.execute(() -> {
                    DataScopeManager.ignoreDataScopeCondition();
                    try {
                        conversationService.generateConversationTitle(conversationId, botId, content, currentAccountId);
                    } catch (Exception e) {
                        log.warn("Failed to generate conversation title asynchronously for conversationId: {}", conversationId, e);
                    } finally {
                        DataScopeManager.restoreDataScopeCondition();
                    }
                });
            } else {
                // 如果没有线程池，直接执行
                conversationService.generateConversationTitle(conversationId, botId, content, currentAccountId);
            }
        }
    }

    @Override
    public Message getMessage(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return null;
        }
        // 先查内存缓存
        for (Message cached : messages) {
            if (messageId.equals(cached.getMessageId())) {
                return cached;
            }
        }
        // 再查 DB：通过 options 中的 af_messageId 匹配
        BotMessage dbMessage = findByAfMessageId(messageId);
        if (dbMessage == null) {
            return null;
        }
        return dbMessage.getContentAsMessage();
    }

    /**
     * 幂等写入消息：已存在（内存缓存或数据库）时跳过。
     * <p>
     * 由框架在每次 Snapshot 保存后同步本轮新增消息时调用，是消息落库的主要入口。
     * </p>
     */
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
        // 先查内存缓存
        for (Message cached : messages) {
            if (messageId.equals(cached.getMessageId())) {
                return false;
            }
        }
        // 再查 DB
        BotMessage existing = findByAfMessageId(messageId);
        if (existing != null) {
            return false;
        }
        addMessage(message);
        return true;
    }

    /**
     * 按版本号乐观更新消息内容。
     */
    @Override
    public boolean updateMessage(Message message, long expectedVersion) {
        if (message == null) {
            return false;
        }
        String messageId = message.getMessageId();
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }
        // 查找现有记录
        BotMessage existing = findByAfMessageId(messageId);
        if (existing == null) {
            return false;
        }
        // 检查版本号
        Map<String, Object> existingOptions = existing.getOptions();
        long currentVersion = 0;
        if (existingOptions != null && existingOptions.get("af_version") instanceof Number) {
            currentVersion = ((Number) existingOptions.get("af_version")).longValue();
        }
        if (currentVersion != expectedVersion) {
            return false;
        }
        // 更新内容和版本号
        existing.setContentAndRole(message);
        existing.setModified(new Date());
        Map<String, Object> options = existing.getOptions();
        if (options == null) {
            options = new HashMap<>();
        }
        options.put("af_version", message.getVersion());
        existing.setOptions(options);
        messageService.updateById(existing);
        return true;
    }

    /**
     * 按消息 ID 反查数据库记录。
     */
    private BotMessage findByAfMessageId(String messageId) {
        return messageService.getOne(QueryWrapper.create()
                .eq(BotMessage::getBotId, botId, true)
                .eq(BotMessage::getAccountId, accountId, true)
                .eq(BotMessage::getConversationId, conversationId, true)
                .like("options", "%af_messageId\":\"" + messageId + "%")
                .limit(1)
        );
    }

    @Override
    public void addMessages(Collection<? extends Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (Message message : messages) {
            addMessage(message);
        }
    }

    private int getRoundNumberForMessage(Message message) {
        if (currentRoundNumber == null) {
            Integer maxRound = getMaxRoundNumber();
            currentRoundNumber = maxRound != null ? maxRound : 0;
        }

        if (message instanceof UserMessage) {
            currentRoundNumber++;
            return currentRoundNumber;
        } else {
            return currentRoundNumber;
        }
    }

    // ========== 以下为 tool_calls 截断相关方法 ==========

    private boolean hasToolCalls(AiMessage aiMsg) {
        return aiMsg.getToolCalls() != null && !aiMsg.getToolCalls().isEmpty();
    }

    /**
     * 截断不完整的 tool_calls，按 tool_call_id 匹配。
     *
     * 对于每个包含 tool_calls 的 AiMessage：
     * - 收集该消息中所有 tool_call 的 id
     * - 检查后续 tool 消息中哪些 id 有响应
     * - 只保留有响应的 tool_calls，移除无响应的
     * - 只保留有对应 tool_call 的 tool 消息
     *
     * 示例：
     * 输入：[AiMsg(tool_calls=[call_1, call_2, call_3]), ToolMsg(call_1, result)]
     * 输出：[AiMsg(tool_calls=[call_1]), ToolMsg(call_1, result)]
     *
     * @param messages 一个轮次中的所有消息
     * @return 截断后的合法消息列表
     */
    private List<Message> truncateIncompleteToolCalls(List<Message> messages) {
        if (messages.isEmpty()) {
            return messages;
        }

        // 1. 收集所有 tool 消息中已响应的 callId
        Set<String> respondedCallIds = new HashSet<>();
        for (Message msg : messages) {
            if (msg instanceof com.agentsflex.core.message.ToolMessage toolMsg) {
                String callId = toolMsg.getToolCallId();
                if (callId != null && !callId.isEmpty()) {
                    respondedCallIds.add(callId);
                }
            }
        }

        // 2. 检查并处理 AiMessage 的 tool_calls 配对情况
        Set<String> keptCallIds = new HashSet<>();
        List<Message> result = new ArrayList<>();

        for (Message msg : messages) {
            if (msg instanceof AiMessage aiMsg && hasToolCalls(aiMsg)) {
                // 过滤：只保留有 tool 响应的 tool_calls
                List<ToolCall> matchedCalls = new ArrayList<>();
                for (ToolCall tc : aiMsg.getToolCalls()) {
                    if (tc.getId() != null && respondedCallIds.contains(tc.getId())) {
                        matchedCalls.add(tc);
                        keptCallIds.add(tc.getId());
                    }
                }

                if (!matchedCalls.isEmpty()) {
                    // 有配对的 tool_calls，复制原消息并仅替换 tool_calls，
                    // 保留 messageId/version/metadata/reasoningContent（messageId 是增量压缩游标的依据）
                    AiMessage truncated = aiMsg.copy();
                    truncated.setToolCalls(matchedCalls);
                    result.add(truncated);
                } else {
                    // 没有 tool_calls 被配对，完全跳过这条 AiMessage
                    continue;
                }
            } else if (msg instanceof com.agentsflex.core.message.ToolMessage toolMsg) {
                // 只保留 tool_call_id 在保留集合中的 tool 消息
                String callId = toolMsg.getToolCallId();
                if (callId != null && keptCallIds.contains(callId)) {
                    result.add(msg);
                }
            } else {
                // UserMessage、SystemMessage 等直接保留
                result.add(msg);
            }
        }

        return result;
    }

    /**
     * 设置用户原始提问内容（不含附件拼接文本），用于落库时保存干净的历史记录。
     */
    public void setOriginalUserContent(String originalUserContent) {
        this.originalUserContent = originalUserContent;
    }

    /**
     * 清空本请求的内存消息缓存。
     * <p>
     * 只清理内存缓存，绝不会删除数据库中的会话历史；这是 ChatMemory 的接口方法，
     * 当前开源链路不会触发。
     * </p>
     */
    @Override
    public void clear() {
        this.messages.clear();
    }

    @Override
    public Object id() {
        return botId + "$$" + conversationId + "$$" + accountId;
    }

}
