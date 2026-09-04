package tech.aiflowy.ai.service.userProfileService;

import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.aiflowy.ai.entity.BotConversation;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.ai.mapper.BotConversationMapper;
import tech.aiflowy.ai.mapper.BotMessageMapper;
import tech.aiflowy.ai.service.ConversationAnalysisService;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *  服务层实现。
 *
 * @author Administrator
 * @since 2025-04-15
 */
@Service("UserProfileService")
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    @Resource
    private BotConversationMapper botConversationMapper;

    @Resource
    private BotMessageMapper botMessageMapper;

    @Resource
    private ConversationAnalysisService conversationAnalysisService;


    /**
     * 分析会话画像
     */
    public void analyzeConversation(BigInteger conversationId, BigInteger botId, BigInteger accountId) {
        conversationAnalysisService.analyzeConversation(conversationId, botId, accountId);
    }

    /**
     * 获取会话分析结果
     */
    public Map<String, Object> getAnalysisResult(BigInteger conversationId) {
        return conversationAnalysisService.getAnalysisResult(conversationId);
    }

    /**
     * 分析指定用户的所有会话画像
     */
    public void analyzeUserConversations(BigInteger accountId) {
        log.info("开始分析用户 {} 的所有会话画像", accountId);
        try {
            DataScopeManager.ignoreDataScopeCondition();
            
            // 查询该用户的所有会话
            QueryWrapper conversationQuery = QueryWrapper.create();
            conversationQuery.eq(BotConversation::getAccountId, accountId);
            List<BotConversation> conversations = botConversationMapper.selectListByQuery(conversationQuery);
            
            if (conversations == null || conversations.isEmpty()) {
                log.info("用户 {} 没有会话记录", accountId);
                return;
            }
            
            int successCount = 0;
            int skipCount = 0;
            
            for (BotConversation conversation : conversations) {
                try {
                    // 检查是否已经分析过
                    Map<String, Object> options = conversation.getOptions();
                    if (options != null && options.get("analysis") != null) {
                        skipCount++;
                        continue;
                    }
                    
                    // 分析会话
                    if (conversation.getBotId() != null) {
                        conversationAnalysisService.analyzeConversation(
                            conversation.getId(), 
                            conversation.getBotId(), 
                            accountId
                        );
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("分析会话 {} 失败", conversation.getId(), e);
                }
            }
            
            log.info("用户 {} 会话分析完成，成功 {} 个，跳过 {} 个", accountId, successCount, skipCount);
        } catch (Exception e) {
            log.error("分析用户 {} 会话画像失败", accountId, e);
        } finally {
            DataScopeManager.restoreDataScopeCondition();
        }
    }

    /**
     * 分析前一天的所有会话画像
     */
    public void analyzeYesterdayConversations() {
        log.info("开始分析前一天的所有会话画像");
        try {
            DataScopeManager.ignoreDataScopeCondition();
            
            // 计算昨天的开始和结束时间
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            Date yesterdayStart = cal.getTime();
            
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            cal.set(java.util.Calendar.SECOND, 59);
            cal.set(java.util.Calendar.MILLISECOND, 999);
            Date yesterdayEnd = cal.getTime();
            
            // 查询昨天创建的会话
            QueryWrapper conversationQuery = QueryWrapper.create();
            conversationQuery.ge(BotConversation::getCreated, yesterdayStart);
            conversationQuery.le(BotConversation::getCreated, yesterdayEnd);
            List<BotConversation> conversations = botConversationMapper.selectListByQuery(conversationQuery);
            
            if (conversations == null || conversations.isEmpty()) {
                log.info("昨天没有会话记录");
                return;
            }
            
            int successCount = 0;
            int skipCount = 0;
            
            for (BotConversation conversation : conversations) {
                try {
                    // 检查是否已经分析过
                    Map<String, Object> options = conversation.getOptions();
                    if (options != null && options.get("analysis") != null) {
                        skipCount++;
                        continue;
                    }
                    
                    // 分析会话
                    if (conversation.getBotId() != null && conversation.getAccountId() != null) {
                        conversationAnalysisService.analyzeConversation(
                            conversation.getId(), 
                            conversation.getBotId(), 
                            conversation.getAccountId()
                        );
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("分析会话 {} 失败", conversation.getId(), e);
                }
            }
            
            log.info("昨天会话分析完成，成功 {} 个，跳过 {} 个", successCount, skipCount);
        } catch (Exception e) {
            log.error("分析昨天会话画像失败", e);
        } finally {
            DataScopeManager.restoreDataScopeCondition();
        }
    }

    /**
     * 分析当天的所有会话画像
     */
    public void analyzeTodayConversations() {
        log.info("开始分析当天的所有会话画像");
        try {
            DataScopeManager.ignoreDataScopeCondition();
            
            // 计算今天的开始和结束时间
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            Date todayStart = cal.getTime();
            
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            cal.set(java.util.Calendar.MINUTE, 59);
            cal.set(java.util.Calendar.SECOND, 59);
            cal.set(java.util.Calendar.MILLISECOND, 999);
            Date todayEnd = cal.getTime();
            
            // 查询今天创建的会话
            QueryWrapper conversationQuery = QueryWrapper.create();
            conversationQuery.ge(BotConversation::getCreated, todayStart);
            conversationQuery.le(BotConversation::getCreated, todayEnd);
            List<BotConversation> conversations = botConversationMapper.selectListByQuery(conversationQuery);
            
            if (conversations == null || conversations.isEmpty()) {
                log.info("今天没有会话记录");
                return;
            }
            
            int successCount = 0;
            int skipCount = 0;
            
            for (BotConversation conversation : conversations) {
                try {
                    // 检查是否已经分析过
                    Map<String, Object> options = conversation.getOptions();
                    if (options != null && options.get("analysis") != null) {
                        skipCount++;
                        continue;
                    }
                    
                    // 检查消息数是否足够
                    QueryWrapper messageQuery = QueryWrapper.create();
                    messageQuery.eq(BotMessage::getConversationId, conversation.getId());
                    messageQuery.eq(BotMessage::getAccountId, conversation.getAccountId());
                    List<BotMessage> messages = botMessageMapper.selectListByQuery(messageQuery);
                    
                    if (messages == null || messages.size() < 3) {
                        skipCount++;
                        continue;
                    }
                    
                    // 分析会话
                    if (conversation.getBotId() != null && conversation.getAccountId() != null) {
                        conversationAnalysisService.analyzeConversation(
                            conversation.getId(), 
                            conversation.getBotId(), 
                            conversation.getAccountId()
                        );
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("分析会话 {} 失败", conversation.getId(), e);
                }
            }
            
            log.info("今天会话分析完成，成功 {} 个，跳过 {} 个", successCount, skipCount);
        } catch (Exception e) {
            log.error("分析今天会话画像失败", e);
        } finally {
            DataScopeManager.restoreDataScopeCondition();
        }
    }

}
