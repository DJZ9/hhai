package tech.aiflowy.ai.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.entity.base.BotToolCallRecordBase;
import tech.aiflowy.system.entity.SysAccount;

/**
 * 实体类。
 *
 * @author wgq
 * @since 2025-06-23
 */

@Table(value = "tb_bot_tool_call_record", comment = "智能体工具调用记录")
public class BotToolCallRecord extends BotToolCallRecordBase {

    @RelationManyToOne(selfField = "createdBy")
    @Column(ignore = true)
    private SysAccount creator;

    public SysAccount getCreator() {
        return creator;
    }

    public void setCreator(SysAccount creator) {
        this.creator = creator;
    }

    /**
     * 获取创建者账号
     */
    public String getCreatorName() {
        if (creator == null) {
            return null;
        }
        return creator.getLoginName() != null ? creator.getLoginName() : creator.getNickname();
    }
}
