package tech.aiflowy.ai.entity;

import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.entity.base.BotBase;

import java.math.BigInteger;
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
    public static final String KEY_EXPOSE_DEEP_THINKING = "exposeDeepThinking";

    // 附件功能配置相关常量
    public static final String KEY_ATTACHMENT_ENABLED = "attachmentEnabled";
    public static final String KEY_VL_MODEL_ID = "vlModelId";

    public static final String KEY_MODEL_OPTIONS_SYSTEM_PROMPT = "systemPrompt";

    public boolean isAnonymousEnabled() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return false;
        }
        Object o = options.get("anonymousEnabled");
        return o != null && (boolean) o;
    }

    /**
     * 是否启用附件功能
     */
    public boolean isAttachmentEnabled() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return false;
        }
        Object o = options.get(KEY_ATTACHMENT_ENABLED);
        return o != null && (boolean) o;
    }

    /**
     * 获取VL模型ID，用于处理图片
     */
    public BigInteger getVlModelId() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return null;
        }
        Object o = options.get(KEY_VL_MODEL_ID);
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            String str = (String) o;
            if (str.isEmpty()) {
                return null;
            }
            // 'current'表示使用当前对话模型，返回null
            if ("current".equals(str)) {
                return null;
            }
            try {
                return new BigInteger(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (o instanceof Number) {
            return BigInteger.valueOf(((Number) o).longValue());
        }
        return null;
    }

    /**
     * 判断是否使用当前对话模型作为VL模型
     */
    public boolean isUseCurrentModelAsVl() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return false;
        }
        Object o = options.get(KEY_VL_MODEL_ID);
        if (o == null) {
            return false;
        }
        if (o instanceof String) {
            return "current".equals((String) o);
        }
        return false;
    }

}
