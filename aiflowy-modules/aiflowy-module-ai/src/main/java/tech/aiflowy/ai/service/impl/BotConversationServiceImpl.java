package tech.aiflowy.ai.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.ai.entity.BotConversation;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.ai.mapper.BotConversationMapper;
import tech.aiflowy.ai.mapper.BotMessageMapper;
import tech.aiflowy.ai.service.BotConversationService;

import javax.annotation.Resource;
import java.math.BigInteger;

/**
 *  服务层实现。
 *
 * @author Administrator
 * @since 2025-04-15
 */
@Service
public class BotConversationServiceImpl extends ServiceImpl<BotConversationMapper, BotConversation> implements BotConversationService {

    @Resource
    private BotConversationMapper botConversationMapper;

    @Resource
    private BotMessageMapper botMessageMapper;

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
}
