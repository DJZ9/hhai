package tech.aiflowy.ai.service.impl;

import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.Message;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.agentsflex.core.util.LocalTokenCounter;
import com.mybatisflex.core.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tech.aiflowy.ai.entity.Bot;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.ai.mapper.BotMapper;
import tech.aiflowy.ai.mapper.BotMessageMapper;
import tech.aiflowy.ai.service.BotMessageService;
import tech.aiflowy.ai.service.ConversationCompressionService;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.core.chat.protocol.MessageRole;

import static tech.aiflowy.ai.entity.table.BotMessageTableDef.BOT_MESSAGE;

import jakarta.annotation.Resource;
import java.math.BigInteger;
import java.util.List;

@Service
public class ConversationCompressionServiceImpl implements ConversationCompressionService {

    private static final Logger log = LoggerFactory.getLogger(ConversationCompressionServiceImpl.class);

    private static final String SUMMARY_SYSTEM_PROMPT = "请对以下AI回复内容进行简洁摘要，用于后续对话上下文理解。要求：\n" +
            "1. 保留核心回答要点和关键信息\n" +
            "2. 简洁明了，不超过150字\n" +
            "3. 直接输出摘要内容，不要添加任何前缀、标题或说明";

    @Resource
    private BotMapper botMapper;

    @Resource
    private BotMessageMapper botMessageMapper;

    @Resource
    private BotMessageService botMessageService;

    @Resource
    private ModelService modelService;

    @Resource(name = "commonExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public void compressConversationHistory(BigInteger botId, BigInteger accountId, BigInteger conversationId) {
        threadPoolTaskExecutor.execute(() -> {
            try {
                doCompressConversationHistory(botId, accountId, conversationId);
            } catch (Exception e) {
                log.error("压缩对话历史失败, botId: {}, conversationId: {}", botId, conversationId, e);
            }
        });
    }

    private void doCompressConversationHistory(BigInteger botId, BigInteger accountId, BigInteger conversationId) {
        Bot bot = botMapper.selectOneById(botId);
        if (bot == null) {
            log.warn("Bot不存在, botId: {}", botId);
            return;
        }

        if (!bot.isCompressionEnabled()) {
            return;
        }

        // 获取最后一条AI消息
        BotMessage lastAiMessage = getLastAiMessage(botId, accountId, conversationId);
        if (lastAiMessage == null) {
            log.debug("没有找到AI消息, botId: {}, conversationId: {}", botId, conversationId);
            return;
        }

        // 如果该消息已经有摘要，跳过
        if (StringUtils.hasText(lastAiMessage.getSummary())) {
            log.debug("消息已有摘要，跳过, messageId: {}", lastAiMessage.getId());
            return;
        }
        // 获取AI回复的完整内容
        AiMessage aiMessage = extractFullContent(lastAiMessage);
        String aiContent = null;
        if (aiMessage != null) {
            aiContent = aiMessage.getFullContent();
        }
        if (!StringUtils.hasText(aiContent)) {
            log.debug("AI消息内容为空，跳过, messageId: {}", lastAiMessage.getId());
            return;
        }

        // 使用LocalTokenCounter计算token数
        int tokenCount = LocalTokenCounter.countPromptTokens(List.of(aiMessage));
        int tokenThreshold = bot.getCompressionTokenThreshold();

        // 小于Token阈值的消息不压缩
        if (tokenCount < tokenThreshold) {
            log.debug("AI消息token数({})小于阈值({})，不压缩, messageId: {}", tokenCount, tokenThreshold, lastAiMessage.getId());
            return;
        }

        log.debug("AI消息token数({})大于或等于阈值({})，进行压缩, messageId: {}", tokenCount, tokenThreshold, lastAiMessage.getId());

        // 生成摘要
        String summary = generateSummaryWithAI(botId, aiContent);
        if (!StringUtils.hasText(summary)) {
            log.warn("AI摘要生成失败, botId: {}, conversationId: {}", botId, conversationId);
            return;
        }

        // 更新AI消息的摘要
        updateMessageSummary(lastAiMessage.getId(), summary);

        log.info("对话历史压缩完成, botId: {}, conversationId: {}, 摘要长度: {}", botId, conversationId, summary.length());
    }

    private BotMessage getLastAiMessage(BigInteger botId, BigInteger accountId, BigInteger conversationId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(BotMessage::getBotId, botId)
                .eq(BotMessage::getAccountId, accountId)
                .eq(BotMessage::getConversationId, conversationId)
                .eq(BotMessage::getRole, MessageRole.ASSISTANT.getValue())
                .orderBy(BOT_MESSAGE.ID.desc())
                .limit(1);

        return botMessageService.getOne(queryWrapper);
    }

    @Override
    public String getExistingSummary(BigInteger botId, BigInteger accountId, BigInteger conversationId) {
        // 不再需要此方法，保留接口兼容
        return null;
    }

    @Override
    public String generateSummaryWithAI(BigInteger botId, String summaryPrompt) {
        Bot bot = botMapper.selectOneById(botId);
        if (bot == null) {
            log.warn("Bot不存在, botId: {}", botId);
            return null;
        }

        // 使用配置的压缩模型ID
        BigInteger modelId = bot.getCompressionModelId();
        if (modelId == null) {
            throw new BusinessException("对话压缩摘要模型不能为空");
        }

        try {
            tech.aiflowy.ai.entity.Model model = modelService.getModelInstance(modelId);
            if (model == null) {
                log.warn("模型不存在, modelId: {}", modelId);
                return null;
            }

            ChatModel chatModel = model.toChatModel(null);
            if (chatModel == null) {
                log.warn("ChatModel创建失败, modelId: {}", modelId);
                return null;
            }

            ChatOptions chatOptions = new ChatOptions();
            chatOptions.setMaxTokens(500);
            chatOptions.setTemperature(0.3f);

            String fullPrompt = SUMMARY_SYSTEM_PROMPT + "\n\n以下是需要摘要的对话内容：\n" + summaryPrompt;
            return chatModel.chat(fullPrompt, chatOptions);
        } catch (Exception e) {
            log.error("AI摘要生成异常, botId: {}", botId, e);
            return null;
        }
    }

    @Override
    public List<BotMessage> getMessagesForCompression(BigInteger botId, BigInteger accountId, BigInteger conversationId, int maxHistoryRounds, int compressionRounds) {
        // 不再需要此方法，保留接口兼容
        return null;
    }

    @Override
    public void updateMessageSummary(BigInteger messageId, String summary) {
        if (messageId == null || !StringUtils.hasText(summary)) {
            return;
        }

        BotMessage message = botMessageService.getById(messageId);
        if (message != null) {
            message.setSummary(summary);
            botMessageService.updateById(message);
        }
    }

    private AiMessage extractFullContent(BotMessage message) {
        try {
            Message msg = message.getContentAsMessage();
            if (msg instanceof AiMessage) {
                return ((AiMessage) msg);
            }
        } catch (Exception e) {
            log.debug("解析消息内容失败, messageId: {}", message.getId());
        }
        return null;
    }

}
