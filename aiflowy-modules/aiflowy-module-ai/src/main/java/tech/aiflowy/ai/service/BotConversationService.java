package tech.aiflowy.ai.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.ai.entity.BotConversation;

import java.math.BigInteger;

/**
 *  服务层。
 *
 * @author Administrator
 * @since 2025-04-15
 */
public interface BotConversationService extends IService<BotConversation> {

    void deleteConversation(String botId, String conversationId, BigInteger accountId);

    void updateConversation(String botId, String conversationId, String title, BigInteger accountId);

    /**
     * 生成会话标题
     * 使用 Bot 绑定的模型根据用户提问生成简短标题
     *
     * @param conversationId 会话ID
     * @param botId          Bot ID
     * @param userContent    用户提问内容
     * @param accountId      用户ID（用于异步线程中验证权限）
     */
    void generateConversationTitle(BigInteger conversationId, BigInteger botId, String userContent, BigInteger accountId);
}
