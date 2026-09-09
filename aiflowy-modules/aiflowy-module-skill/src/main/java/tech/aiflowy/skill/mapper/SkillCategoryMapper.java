package tech.aiflowy.skill.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import tech.aiflowy.skill.entity.SkillCategory;

/**
 * 技能分类映射层。
 *
 * @author wgq
 * @since 2026-07-21
 */
@Mapper
public interface SkillCategoryMapper extends BaseMapper<SkillCategory> {

}