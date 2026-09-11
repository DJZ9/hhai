package tech.aiflowy.ai.service.impl;

import com.agentsflex.core.model.chat.ChatOptions;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tech.aiflowy.ai.entity.Bot;
import tech.aiflowy.ai.entity.BotConversation;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.mapper.BotConversationMapper;
import tech.aiflowy.ai.mapper.BotMapper;
import tech.aiflowy.ai.mapper.BotMessageMapper;
import tech.aiflowy.ai.service.BotConversationService;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.ai.service.userProfileService.DataScopeManager;

import jakarta.annotation.Resource;
import java.math.BigInteger;
import java.util.Date;

/**
 *  服务层实现。
 *
 * @author Administrator
 * @since 2025-04-15
 */
@Service
public class BotConversationServiceImpl extends ServiceImpl<BotConversationMapper, BotConversation> implements BotConversationService {

    private static final Logger log = LoggerFactory.getLogger(BotConversationServiceImpl.class);
    private static final int MAX_TITLE_LENGTH = 15; // 会话标题最大长度

    @Resource
    private BotConversationMapper botConversationMapper;

    @Resource
    private BotMessageMapper botMessageMapper;

    @Resource
    private BotMapper botMapper;

    @Resource
    private ModelService modelService;

    /**
     * 删除指定会话
     */
    @Override
    public void deleteConversation(String botId, String conversationId, BigInteger accountId) {
        QueryWrapper cqw = QueryWrapper.create();
        cqw.eq(BotConversation::getBotId, botId);
        cqw.eq(BotConversation::getId, conversationId);
        cqw.eq(BotConversation::getAccountId, accountId);
        botConversationMapper.deleteByQuery(cqw);
        // 删除消息记录中的数据
        QueryWrapper mqw = QueryWrapper.create();
        mqw.eq(BotMessage::getBotId, botId);
        mqw.eq(BotMessage::getConversationId, conversationId);
        mqw.eq(BotMessage::getAccountId, accountId);
        botMessageMapper.deleteByQuery(mqw);
    }

    @Override
    public void updateConversation(String botId, String conversationId, String title, BigInteger accountId) {
        QueryWrapper cqw = QueryWrapper.create();
        cqw.eq(BotConversation::getBotId, botId);
        cqw.eq(BotConversation::getId, conversationId);
        cqw.eq(BotConversation::getAccountId, accountId);
        BotConversation update = new BotConversation();
        update.setTitle(title);
        botConversationMapper.updateByQuery(update, cqw);
    }

    /**
     * 生成会话标题
     * 使用Bot绑定的模型根据用户提问生成简短标题
     */
    @Override
    public void generateConversationTitle(BigInteger conversationId, BigInteger botId, String userContent, BigInteger accountId) {
        if (!StringUtils.hasText(userContent)) {
            return;
        }

        try {
            DataScopeManager.ignoreDataScopeCondition();
            // 检查会话是否已有标题
            QueryWrapper cqw = QueryWrapper.create();
            cqw.eq(BotConversation::getId, conversationId);
            cqw.eq(BotConversation::getBotId, botId);
            cqw.eq(BotConversation::getAccountId, accountId);
            BotConversation conversation = botConversationMapper.selectOneByQuery(cqw);
            if (conversation == null || StringUtils.hasText(conversation.getTitle())) {
                return;
            }

            // 获取Bot信息
            Bot bot = botMapper.selectOneById(botId);
            if (bot == null) {
                // 回退到简单截取
                saveSimpleTitle(conversationId, userContent, accountId);
                return;
            }

            // 获取Bot绑定的模型
            BigInteger modelId = bot.getModelId();
            if (modelId == null) {
                // 回退到简单截取
                saveSimpleTitle(conversationId, userContent, accountId);
                return;
            }

            Model model = modelService.getModelInstance(modelId);
            if (model == null) {
                // 回退到简单截取
                saveSimpleTitle(conversationId, userContent, accountId);
                return;
            }

            // 创建ChatModel
            com.agentsflex.core.model.chat.ChatModel chatModel = model.toChatModel(null);
            if (chatModel == null) {
                // 回退到简单截取
                saveSimpleTitle(conversationId, userContent, accountId);
                return;
            }

            // 构建提示词
            String prompt = "请为以下用户提问生成一个简短的标题（不超过15个字），只返回标题内容，不要有任何解释，你不要做任何思考过程：\n\n" + userContent;

            ChatOptions options = new ChatOptions();
            options.setTemperature(0.3f);

            // 调用模型生成标题
            String title = chatModel.chat(prompt, options);

            if (StringUtils.hasText(title)) {
                // 清理标题（去除换行、引号等）
                title = title.trim().replaceAll("[\"'\\n\\r]", "");
                if (title.length() > MAX_TITLE_LENGTH) {
                    title = title.substring(0, MAX_TITLE_LENGTH) + "...";
                }
                saveTitle(conversationId, title, accountId);
            } else {
                // 回退到简单截取
                saveSimpleTitle(conversationId, userContent, accountId);
            }
        } catch (Exception e) {
            log.warn("使用AI模型为conversationId生成对话标题失败: {}", conversationId, e);
            // 回退到简单截取
            saveSimpleTitle(conversationId, userContent, accountId);
        } finally {
            DataScopeManager.restoreDataScopeCondition();
        }
    }

    /**
     * 保存简单截取的标题
     */
    private void saveSimpleTitle(BigInteger conversationId, String content, BigInteger accountId) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        String title = content.length() > MAX_TITLE_LENGTH
                ? content.substring(0, MAX_TITLE_LENGTH) + "..."
                : content;
        saveTitle(conversationId, title, accountId);
    }

    /**
     * 保存会话标题
     */
    private void saveTitle(BigInteger conversationId, String title, BigInteger accountId) {
        try {
            // 使用accountId验证权限，避免SA-Token在非web上下文获取失败
            QueryWrapper cqw = QueryWrapper.create();
            cqw.eq(BotConversation::getId, conversationId);
            cqw.eq(BotConversation::getAccountId, accountId);
            BotConversation conversation = botConversationMapper.selectOneByQuery(cqw);
            if (conversation != null && !StringUtils.hasText(conversation.getTitle())) {
                conversation.setTitle(title);
                conversation.setModified(new Date());
                botConversationMapper.update(conversation);
            }
        } catch (Exception e) {
            log.warn("Failed to save conversation title for conversationId: {}", conversationId, e);
        }
    }
}
