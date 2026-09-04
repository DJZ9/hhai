package tech.aiflowy.ai.service.userProfileService;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 会话画像定时任务配置
 */
@Configuration
@EnableScheduling
public class ConversationAnalysisScheduleConfig {

    private static final Logger log = LoggerFactory.getLogger(ConversationAnalysisScheduleConfig.class);

    @Resource
    private UserProfileService userProfileService;

    /**
     * 每天凌晨3点分析前一天的所有会话画像
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void analyzeYesterdayConversations() {
        log.info("开始执行定时任务：分析前一天的所有会话画像");
        try {
            userProfileService.analyzeYesterdayConversations();
        } catch (Exception e) {
            log.error("定时任务执行失败", e);
        }
    }
}
