package tech.aiflowy.ai.agentsflex;

import com.agentsflex.core.model.chat.ChatOptions;
import java.util.Map;

/**
 * ChatOptions 构建工具类。
 * <p>
 * 根据前端传入的 Map 配置构建 {@link ChatOptions}，封装温度、TopP、
 * 最大回复长度、思考模式等参数的解析逻辑，供各业务模块统一复用。
 * </p>
 */
public class ChatOptionsBuilder {

    private ChatOptionsBuilder() {
        // 私有构造，禁止实例化
    }

    /**
     * 根据大模型配置 Map 构建 {@link ChatOptions}。
     * 各参数均需对应启用开关为 true 且值非空时才会设置，未启用或未配置的参数保持默认值。
     * <p>
     * 兼容规则：启用开关不存在（历史 Bot 只配置了参数值）时，视为启用，保证迁移前的行为不变；
     * 启用开关存在时严格按开关取值。{@code topK} 无 UI 配置项，存在即透传。
     * </p>
     *
     * @param llmOptions 大模型参数配置，允许为 null
     * @return 构建完成的 {@link ChatOptions}，当 llmOptions 为 null 时返回默认配置
     */
    public static ChatOptions getChatOptions(Map<String, Object> llmOptions) {
        ChatOptions defaultOptions = new ChatOptions();
        if (llmOptions != null) {
            // 温度参数：检查是否启用
            Object temperature = llmOptions.get("temperature");
            if (isEnabled(llmOptions, "temperatureEnabled", temperature)) {
                defaultOptions.setTemperature(Float.parseFloat(String.valueOf(temperature)));
            }

            // TopP参数，检查是否启用
            Object topP = llmOptions.get("topP");
            if (isEnabled(llmOptions, "topPEnabled", topP)) {
                defaultOptions.setTopP(Float.parseFloat(String.valueOf(topP)));
            }

            // 最大回复长度：检查是否启用
            Object maxReplyLength = llmOptions.get("maxReplyLength");
            if (isEnabled(llmOptions, "maxReplyLengthEnabled", maxReplyLength)) {
                defaultOptions.setMaxTokens(Integer.parseInt(String.valueOf(maxReplyLength)));
            }

            // TopK：没有 UI 配置项，存在即透传
            Object topK = llmOptions.get("topK");
            if (topK != null) {
                defaultOptions.setTopK(Integer.parseInt(String.valueOf(topK)));
            }

            // 思考模式（保持不变）
            Object thinkingEnabled = llmOptions.get("thinkingEnabled");
            if (thinkingEnabled != null) {
                defaultOptions.setThinkingEnabled(Boolean.parseBoolean(String.valueOf(thinkingEnabled)));
            }
        }
        return defaultOptions;
    }

    /**
     * 判断某个参数是否启用：
     * 开关键存在时以开关为准；开关键不存在但参数值存在时视为启用（兼容历史配置）。
     */
    private static boolean isEnabled(Map<String, Object> llmOptions, String enabledKey, Object value) {
        if (value == null) {
            return false;
        }
        Object enabled = llmOptions.get(enabledKey);
        if (enabled == null) {
            return true;
        }
        return Boolean.parseBoolean(String.valueOf(enabled));
    }
}
