package tech.aiflowy.skill.dto;

import java.math.BigInteger;
import java.util.List;

/**
 * Bot 技能关联更新请求 DTO。
 *
 * @author wgq
 * @since 2026-07-21
 */
public class BotSkillUpdateRequest {

    /**
     * Bot ID
     */
    private BigInteger botId;

    /**
     * Skill ID 列表
     */
    private List<BigInteger> skillIds;

    public BigInteger getBotId() {
        return botId;
    }

    public void setBotId(BigInteger botId) {
        this.botId = botId;
    }

    public List<BigInteger> getSkillIds() {
        return skillIds;
    }

    public void setSkillIds(List<BigInteger> skillIds) {
        this.skillIds = skillIds;
    }
}