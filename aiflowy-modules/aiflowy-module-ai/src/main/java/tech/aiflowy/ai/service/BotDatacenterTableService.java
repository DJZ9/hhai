package tech.aiflowy.ai.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.ai.entity.BotDatacenterTable;

import java.math.BigInteger;
import java.util.List;

/**
 * bot绑定的数据中枢 服务层。
 *
 * @author John
 * @since 2026-03-02
 */
public interface BotDatacenterTableService extends IService<BotDatacenterTable> {

    /**
     * 更新 Bot 数据中枢绑定
     *
     * @param botId               Bot ID
     * @param datacenterTableIds  数据中枢表 ID 列表
     */
    void updateBotDatacenterTableIds(BigInteger botId, List<BigInteger> datacenterTableIds);
}
