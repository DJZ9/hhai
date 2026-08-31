package tech.aiflowy.ai.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.ai.entity.BotConversation;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.ai.entity.BotRecentlyUsed;
import tech.aiflowy.ai.mapper.BotRecentlyUsedMapper;
import tech.aiflowy.ai.service.BotConversationService;
import tech.aiflowy.ai.service.BotMessageService;
import tech.aiflowy.ai.service.BotRecentlyUsedService;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.List;

/**
 * 最近使用 服务层实现。
 *
 * @author ArkLight
 * @since 2025-12-18
 */
@Service
public class BotRecentlyUsedServiceImpl extends ServiceImpl<BotRecentlyUsedMapper, BotRecentlyUsed>  implements BotRecentlyUsedService {

    @Resource
    private BotMessageService botMessageService;

    @Resource
    private BotConversationService botConversationService;

    @Override
    public void removeRecentlyAllUsed(List<BigInteger> botIds) {
        this.remove(QueryWrapper.create().in(BotRecentlyUsed::getBotId,botIds));
        botMessageService.remove(QueryWrapper.create().in(BotMessage::getBotId,botIds));
        botConversationService.remove(QueryWrapper.create().in(BotConversation::getBotId,botIds));
    }
}
