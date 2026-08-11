package tech.aiflowy.ai.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.ai.entity.BotSubagent;

/**
 * bot子代理关联表 服务层。
 *
 * @author 12076
 * @since 2026-06-08
 */
public interface BotSubagentService extends IService<BotSubagent> {

    void saveBotAndSubagentIds(java.math.BigInteger botId, java.math.BigInteger[] subagentIds);
}
