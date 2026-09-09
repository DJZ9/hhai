package tech.aiflowy.skill.entity;

import com.mybatisflex.annotation.Table;
import tech.aiflowy.skill.entity.base.SkillBase;

/**
 * 技能 实体类。
 *
 * @author wgq
 * @since 2026-07-21
 */
@Table(value = "tb_skill", comment = "技能")
public class Skill extends SkillBase {

    private static final long serialVersionUID = 1L;

}