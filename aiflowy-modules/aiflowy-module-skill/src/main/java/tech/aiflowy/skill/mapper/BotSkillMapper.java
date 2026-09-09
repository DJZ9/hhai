package tech.aiflowy.skill.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import tech.aiflowy.skill.entity.BotSkill;

/**
 * 机器人技能关联映射层。
 *
 * @author wgq
 * @since 2026-07-21
 */
@Mapper
public interface BotSkillMapper extends BaseMapper<BotSkill> {

}