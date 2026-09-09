package tech.aiflowy.skill.entity;

import com.mybatisflex.annotation.Table;
import tech.aiflowy.skill.entity.base.SkillCategoryBase;

/**
 * 技能分类 实体类。
 *
 * @author wgq
 * @since 2026-07-21
 */
@Table(value = "tb_skill_category", comment = "技能分类")
public class SkillCategory extends SkillCategoryBase {

    private static final long serialVersionUID = 1L;

}