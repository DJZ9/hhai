package tech.aiflowy.ai.entity;

import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.entity.base.BotPluginBase;


/**
 *  实体类。
 *
 * @author michael
 * @since 2025-04-07
 */
@Table("tb_bot_plugin")
public class BotPlugin extends BotPluginBase {

    @RelationOneToOne(selfField = "pluginItemId", targetField = "id")
    private PluginItem pluginItem;

    public PluginItem getPluginItem() {
        return pluginItem;
    }

    public void setPluginItem(PluginItem pluginItem) {
        this.pluginItem = pluginItem;
    }
}
