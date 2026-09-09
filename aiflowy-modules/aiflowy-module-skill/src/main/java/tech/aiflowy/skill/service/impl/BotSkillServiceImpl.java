package tech.aiflowy.skill.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.aiflowy.skill.entity.BotSkill;
import tech.aiflowy.skill.mapper.BotSkillMapper;
import tech.aiflowy.skill.service.BotSkillService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 机器人技能关联 服务层实现。
 *
 * @author wgq
 * @since 2026-07-21
 */
@Service
public class BotSkillServiceImpl extends ServiceImpl<BotSkillMapper, BotSkill> implements BotSkillService {

    @Override
    public List<BotSkill> listByBotId(BigInteger botId) {
        if (botId == null) {
            return new ArrayList<>();
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(BotSkill::getBotId, botId);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBotSkills(BigInteger botId, List<BigInteger> skillIds) {
        if (botId == null) {
            throw new IllegalArgumentException("Bot ID 不能为空");
        }

        // 删除旧的关联
        QueryWrapper deleteWrapper = QueryWrapper.create()
                .eq(BotSkill::getBotId, botId);
        remove(deleteWrapper);

        // 插入新的关联
        if (skillIds != null && !skillIds.isEmpty()) {
            List<BotSkill> botSkills = new ArrayList<>();
            for (BigInteger skillId : skillIds) {
                if (skillId != null) {
                    BotSkill botSkill = new BotSkill();
                    botSkill.setBotId(botId);
                    botSkill.setSkillId(skillId);
                    botSkill.setCreatedAt(new Date());
                    botSkills.add(botSkill);
                }
            }
            if (!botSkills.isEmpty()) {
                saveBatch(botSkills);
            }
        }
    }
}