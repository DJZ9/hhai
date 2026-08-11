package tech.aiflowy.ai.entity;

import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.entity.base.BotSubagentBase;


/**
 * bot子代理关联表 实体类。
 *
 * @author 12076
 * @since 2026-06-08
 */
@Table(value = "tb_bot_subagent", comment = "bot子代理关联表")
public class BotSubagent extends BotSubagentBase {

    @RelationOneToOne(selfField = "subagentId", targetField = "id")
    private Bot subagent;

    public Bot getSubagent() {
        return subagent;
    }

    public void setSubagent(Bot subagent) {
        this.subagent = subagent;
    }
}
