package tech.aiflowy.ai.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.aiflowy.ai.entity.BotDatacenterTable;
import tech.aiflowy.ai.mapper.BotDatacenterTableMapper;
import tech.aiflowy.ai.service.BotDatacenterTableService;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * bot绑定的数据中枢 服务层实现。
 *
 * @author John
 * @since 2026-03-02
 */
@Service
public class BotDatacenterTableServiceImpl extends ServiceImpl<BotDatacenterTableMapper, BotDatacenterTable> implements BotDatacenterTableService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBotDatacenterTableIds(BigInteger botId, List<BigInteger> datacenterTableIds) {
        // 1. 删除该 Bot 原有的所有数据中枢绑定
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(BotDatacenterTable::getBotId, botId);
        this.remove(wrapper);

        // 2. 如果 datacenterTableIds 为空，直接返回
        if (datacenterTableIds == null || datacenterTableIds.isEmpty()) {
            return;
        }

        // 3. 去重，避免重复绑定
        List<BigInteger> distinctIds = datacenterTableIds.stream()
                .distinct()
                .collect(Collectors.toList());

        // 4. 批量插入新的绑定关系
        for (BigInteger tableId : distinctIds) {
            BotDatacenterTable entity = new BotDatacenterTable();
            entity.setBotId(botId);
            entity.setTableId(tableId);
            this.save(entity);
        }
    }
}
