package tech.aiflowy.skill.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import tech.aiflowy.skill.entity.Skill;

/**
 * 技能映射层。
 *
 * @author wgq
 * @since 2026-07-21
 */
@Mapper
public interface SkillMapper extends BaseMapper<Skill> {

}