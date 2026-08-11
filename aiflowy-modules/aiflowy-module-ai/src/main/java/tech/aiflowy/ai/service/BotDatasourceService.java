package tech.aiflowy.ai.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.ai.entity.BotDatasource;

import java.math.BigInteger;
import java.util.List;

/**
 * bot绑定的数据源 服务层。
 *
 * @author John
 * @since 2026-03-02
 */
public interface BotDatasourceService extends IService<BotDatasource> {

    /**
     * 更新 Bot 数据源绑定
     *
     * @param botId          Bot ID
     * @param datasourceIds  数据源 ID 列表
     */
    void updateBotDatasourceIds(BigInteger botId, List<BigInteger> datasourceIds);
}
