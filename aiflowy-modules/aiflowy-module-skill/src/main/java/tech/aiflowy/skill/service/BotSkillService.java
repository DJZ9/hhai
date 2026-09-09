package tech.aiflowy.skill.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.skill.entity.BotSkill;

import java.math.BigInteger;
import java.util.List;

/**
 * 机器人技能关联 服务层。
 *
 *  @author wgq
 *  @since 2026-07-21
 */
public interface BotSkillService extends IService<BotSkill> {

    /**
     * 根据 Bot ID 查询关联的 Skills
     *
     * @param botId Bot ID
     * @return 关联的 BotSkill 列表
     */
    List<BotSkill> listByBotId(BigInteger botId);

    /**
     * 更新 Bot 关联的 Skills
     *
     * @param botId Bot ID
     * @param skillIds Skill ID 列表
     */
    void updateBotSkills(BigInteger botId, List<BigInteger> skillIds);
}