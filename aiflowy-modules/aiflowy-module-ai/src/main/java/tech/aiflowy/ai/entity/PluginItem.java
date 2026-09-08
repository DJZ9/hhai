package tech.aiflowy.ai.entity;

import com.agentsflex.core.model.chat.tool.Tool;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.agentsflex.tool.PluginTool;
import tech.aiflowy.ai.entity.base.PluginItemBase;

import java.math.BigInteger;


/**
 *  实体类。
 *
 * @author Administrator
 * @since 2025-04-27
 */
@Table("tb_plugin_item")
public class PluginItem extends PluginItemBase {

    @Column(ignore = true)
    private boolean joinBot;

    @Column(ignore = true)
    private BigInteger recordId;

    @Column(ignore = true)
    private String outputTemplate;

    public boolean isJoinBot() {
        return joinBot;
    }

    public void setJoinBot(boolean joinBot) {
        this.joinBot = joinBot;
    }

    public BigInteger getRecordId() {
        return recordId;
    }

    public void setRecordId(BigInteger recordId) {
        this.recordId = recordId;
    }

    public String getOutputTemplate() {
        return outputTemplate;
    }

    public void setOutputTemplate(String outputTemplate) {
        this.outputTemplate = outputTemplate;
    }

    public Tool toFunction() {
        return new PluginTool(this);
    }

}
