package tech.aiflowy.skill.entity;

import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.skill.entity.base.BotSkillBase;

/**
 * 机器人技能关联 实体类。
 *
 * @author wgq
 * @since 2026-07-21
 */
@Table(value = "tb_bot_skill", comment = "机器人技能关联")
public class BotSkill extends BotSkillBase {

    private static final long serialVersionUID = 1L;

    @RelationOneToOne(selfField = "skillId", targetField = "id")
    private Skill skill;

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }
}