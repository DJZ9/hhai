package tech.aiflowy.ai.entity;

import tech.aiflowy.ai.entity.base.BotBase;
import com.mybatisflex.annotation.Table;

import java.util.Map;

/**
 * 实体类。
 *
 * @author michael
 * @since 2024-08-23
 */

@Table("tb_bot")
public class Bot extends BotBase {

    public static final String KEY_SYSTEM_PROMPT = "systemPrompt";
    public static final String KEY_MAX_MESSAGE_COUNT = "maxMessageCount";
    public static final String KEY_ENABLE_DEEP_THINKING = "enableDeepThinking";

    public boolean isAnonymousEnabled() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return false;
        }
        Object o = options.get("anonymousEnabled");
        return o != null && (boolean) o;
    }

}
