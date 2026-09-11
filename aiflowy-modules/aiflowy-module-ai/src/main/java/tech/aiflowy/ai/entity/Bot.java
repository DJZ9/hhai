package tech.aiflowy.ai.entity;

import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.entity.base.BotBase;
import tech.aiflowy.common.util.MapUtil;

import java.math.BigInteger;
import java.util.HashMap;
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

    // 大模型配置相关常量（存储在 modelOptions 中，可由用户配置）
    public static final String KEY_TEMPERATURE = "temperature";
    public static final String KEY_TEMPERATURE_ENABLED = "temperatureEnabled";
    public static final String KEY_TOP_P = "topP";
    public static final String KEY_TOP_P_ENABLED = "topPEnabled";
    public static final String KEY_MAX_REPLY_LENGTH = "maxReplyLength";
    public static final String KEY_MAX_REPLY_LENGTH_ENABLED = "maxReplyLengthEnabled";
    public static final String KEY_TOP_K = "topK";

    // 附件功能配置相关常量
    public static final String KEY_ATTACHMENT_ENABLED = "attachmentEnabled";
    public static final String KEY_VL_MODEL_ID = "vlModelId";

    public static final String KEY_MODEL_OPTIONS_SYSTEM_PROMPT = "systemPrompt";

    // ===== Agent 执行参数固定默认值（开源版不开放给用户自定义）=====

    // 执行上下文
    private static final int DEFAULT_MAX_ITERATIONS = 200;
    private static final int DEFAULT_MAX_STEPS = 1000;
    private static final int DEFAULT_MAX_ATTACHED_TURNS = 10;
    private static final long DEFAULT_MAX_ATTACHED_TOKENS = 0L;
    private static final int DEFAULT_MAX_ATTACHED_MESSAGES = 100;
    private static final String DEFAULT_TOOL_ERROR_STRATEGY = "RETURN_ERROR_TO_MODEL";

    // 预算控制（0 表示不限制）
    private static final long DEFAULT_MAX_INPUT_TOKENS = 0L;
    private static final long DEFAULT_MAX_OUTPUT_TOKENS = 0L;
    private static final long DEFAULT_MAX_TOTAL_TOKENS = 0L;
    private static final int DEFAULT_MAX_TOOL_CALLS = 200;
    private static final long DEFAULT_MAX_DURATION_MILLIS = 1_200_000L;

    // 重试（默认不重试）
    private static final int DEFAULT_MAX_RETRIES = 0;
    private static final double DEFAULT_RETRY_MULTIPLIER = 2.0;
    private static final long DEFAULT_INITIAL_DELAY_MILLIS = 0L;
    private static final long DEFAULT_MAX_DELAY_MILLIS = 0L;

    // 摘要压缩（固定：按 Turn 触发、阈值 2 轮、保留最近 2 轮原文、摘要模型使用当前对话模型）
    private static final String DEFAULT_COMPRESSION_DECIDER = "PENDING_TURNS";
    private static final int DEFAULT_COMPRESSION_TURN_THRESHOLD = 2;
    private static final int DEFAULT_COMPRESSION_KEEP_RECENT_TURNS = 2;
    private static final int DEFAULT_COMPRESSION_ROUNDS = 2;
    private static final String DEFAULT_COMPRESSION_FAILURE_STRATEGY = "FAIL";
    private static final boolean DEFAULT_COMPACT_COMPLETED_TOOL_TURNS = true;
    private static final String DEFAULT_COMPRESSION_INSTRUCTION =
            "请用中文总结历史事实，保留业务 ID、时间、用户约束和未完成事项，不要编造信息。";

    // 高级执行策略（0 表示不限制）
    private static final long DEFAULT_MODEL_CALL_TIMEOUT_MILLIS = 0L;
    private static final long DEFAULT_TOOL_EXECUTION_TIMEOUT_MILLIS = 0L;
    private static final long DEFAULT_EXTERNAL_TOOL_TIMEOUT_MILLIS = 0L;
    private static final long DEFAULT_APPROVAL_TIMEOUT_MILLIS = 0L;
    private static final long DEFAULT_USER_INPUT_TIMEOUT_MILLIS = 0L;
    private static final String DEFAULT_SUSPENSION_EXPIRATION_STRATEGY = "REJECT_RESUME";
    private static final long DEFAULT_TOOL_RESULT_MAX_CHARACTERS = 0L;
    private static final long DEFAULT_EXTERNAL_TOOL_RESULT_MAX_CHARACTERS = 0L;
    private static final String DEFAULT_TOOL_RESULT_OVERFLOW_STRATEGY = "FAIL";
    private static final String DEFAULT_TOOL_EXECUTION_MODE = "SEQUENTIAL";
    private static final int DEFAULT_MAX_PARALLEL_TOOL_CALLS = 8;
    private static final String DEFAULT_PARALLEL_FAILURE_STRATEGY = "FAIL_FAST";
    private static final String DEFAULT_INTERRUPTED_TOOL_MESSAGE_TEMPLATE = "Tool call was not completed: {reason}";
    private static final String DEFAULT_INTERRUPTED_TURN_MESSAGE_TEMPLATE = "The previous AgentTurn ended before completion: {reason}";
    private static final String DEFAULT_CANCELLATION_REASON = "turn cancelled by caller";

    /**
     * 获取默认的大模型配置（modelOptions），创建新 Bot 时使用。
     * <p>
     * 仅包含开放给用户配置的模型参数与历史条数；Agent 执行上下文、预算、重试、压缩与高级执行策略
     * 由后端固定默认值提供（见各类 getter），不写入 modelOptions，也不受用户配置影响。
     * </p>
     */
    public static Map<String, Object> getDefaultLlmOptions() {
        Map<String, Object> defaultLlmOptions = new HashMap<>();
        // 模型参数
        defaultLlmOptions.put(KEY_TEMPERATURE, 0.3);
        defaultLlmOptions.put(KEY_TEMPERATURE_ENABLED, false);
        defaultLlmOptions.put(KEY_TOP_P, 0.9);
        defaultLlmOptions.put(KEY_TOP_P_ENABLED, false);
        defaultLlmOptions.put(KEY_MAX_REPLY_LENGTH, 20000);
        defaultLlmOptions.put(KEY_MAX_REPLY_LENGTH_ENABLED, false);
        // 上下文窗口：单次请求模型时最多携带的历史消息条数
        defaultLlmOptions.put(KEY_MAX_MESSAGE_COUNT, DEFAULT_MAX_ATTACHED_MESSAGES);
        return defaultLlmOptions;
    }

    /**
     * 获取默认的 Bot 选项配置（options），创建新 Bot 时使用。
     * 包含深度思考展示、附件功能、VL 模型等基础配置。
     */
    public static Map<String, Object> getDefaultOptions() {
        Map<String, Object> defaultOptions = new HashMap<>();
        // 默认开启深度思考
        defaultOptions.put(KEY_EXPOSE_DEEP_THINKING, true);
        // 默认开启附件功能
        defaultOptions.put(KEY_ATTACHMENT_ENABLED, true);
        // 默认使用当前对话模型作为VL模型
        defaultOptions.put(KEY_VL_MODEL_ID, "current");
        return defaultOptions;
    }

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
     * 获取 VL 模型 ID，用于处理图片
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
     * 判断是否使用当前对话模型作为 VL 模型
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

    // ===== Agent 执行参数 getter（固定默认值，不读取用户配置）=====

    /**
     * 获取最大模型迭代次数（固定默认 200）
     */
    public int getMaxIterations() {
        return DEFAULT_MAX_ITERATIONS;
    }

    /**
     * 获取最大步骤数（固定默认 1000）
     */
    public int getMaxSteps() {
        return DEFAULT_MAX_STEPS;
    }

    /**
     * 获取挂载 Turn 数（固定默认 10）
     */
    public int getMaxAttachedTurns() {
        return DEFAULT_MAX_ATTACHED_TURNS;
    }

    /**
     * 获取挂载 Token 上限（固定默认 0，表示不限制）
     */
    public long getMaxAttachedTokens() {
        return DEFAULT_MAX_ATTACHED_TOKENS;
    }

    /**
     * 获取挂载消息条数：以「历史条数」(maxMessageCount) 作为上下文窗口，
     * 非法值（缺失、0、负数）回退到默认 100。
     */
    public int getMaxAttachedMessages() {
        int value = MapUtil.getInteger(getModelOptions(), KEY_MAX_MESSAGE_COUNT, DEFAULT_MAX_ATTACHED_MESSAGES);
        return value > 0 ? value : DEFAULT_MAX_ATTACHED_MESSAGES;
    }

    /**
     * 获取工具最终失败策略（固定默认 RETURN_ERROR_TO_MODEL）
     */
    public String getToolErrorStrategy() {
        return DEFAULT_TOOL_ERROR_STRATEGY;
    }

    /**
     * 获取输入 Token 预算（固定默认 0，表示不限制）
     */
    public long getMaxInputTokens() {
        return DEFAULT_MAX_INPUT_TOKENS;
    }

    /**
     * 获取输出 Token 预算（固定默认 0，表示不限制）
     */
    public long getMaxOutputTokens() {
        return DEFAULT_MAX_OUTPUT_TOKENS;
    }

    /**
     * 获取总 Token 预算（固定默认 0，表示不限制）
     */
    public long getMaxTotalTokens() {
        return DEFAULT_MAX_TOTAL_TOKENS;
    }

    /**
     * 获取工具调用次数上限（固定默认 200）
     */
    public int getMaxToolCalls() {
        return DEFAULT_MAX_TOOL_CALLS;
    }

    /**
     * 获取执行时长上限（固定默认 1200000 毫秒）
     */
    public long getMaxDurationMillis() {
        return DEFAULT_MAX_DURATION_MILLIS;
    }

    /**
     * 获取最大重试次数（固定默认 0，表示不重试）
     */
    public int getMaxRetries() {
        return DEFAULT_MAX_RETRIES;
    }

    /**
     * 获取退避倍率（固定默认 2.0）
     */
    public double getRetryMultiplier() {
        return DEFAULT_RETRY_MULTIPLIER;
    }

    /**
     * 获取初始重试间隔（固定默认 0 毫秒）
     */
    public long getInitialDelayMillis() {
        return DEFAULT_INITIAL_DELAY_MILLIS;
    }

    /**
     * 获取最大重试间隔（固定默认 0 毫秒）
     */
    public long getMaxDelayMillis() {
        return DEFAULT_MAX_DELAY_MILLIS;
    }

    /**
     * 获取压缩触发条件（固定默认 PENDING_TURNS）
     */
    public String getCompressionDecider() {
        return DEFAULT_COMPRESSION_DECIDER;
    }

    /**
     * 获取压缩 Turn 阈值（固定默认 2 轮）
     */
    public int getCompressionTurnThreshold() {
        return DEFAULT_COMPRESSION_TURN_THRESHOLD;
    }

    /**
     * 获取压缩保留最近 Turn 数（固定默认 2 轮）
     */
    public int getCompressionKeepRecentTurns() {
        return DEFAULT_COMPRESSION_KEEP_RECENT_TURNS;
    }

    /**
     * 获取压缩轮数（固定默认 2 轮）
     */
    public int getCompressionRounds() {
        return DEFAULT_COMPRESSION_ROUNDS;
    }

    /**
     * 获取压缩失败策略（固定默认 FAIL）
     */
    public String getCompressionFailureStrategy() {
        return DEFAULT_COMPRESSION_FAILURE_STRATEGY;
    }

    /**
     * 是否压缩时归并已完成工具 Turn（固定默认 true）
     */
    public boolean isCompactCompletedToolTurns() {
        return DEFAULT_COMPACT_COMPLETED_TOOL_TURNS;
    }

    /**
     * 获取压缩摘要指令（固定默认内置中文摘要规则）
     */
    public String getCompressionInstruction() {
        return DEFAULT_COMPRESSION_INSTRUCTION;
    }

    /**
     * 获取模型调用超时（固定默认 0，表示不限制）
     */
    public long getModelCallTimeoutMillis() {
        return DEFAULT_MODEL_CALL_TIMEOUT_MILLIS;
    }

    /**
     * 获取本地工具超时（固定默认 0，表示不限制）
     */
    public long getToolExecutionTimeoutMillis() {
        return DEFAULT_TOOL_EXECUTION_TIMEOUT_MILLIS;
    }

    /**
     * 获取外部工具超时（固定默认 0，表示不限制）
     */
    public long getExternalToolTimeoutMillis() {
        return DEFAULT_EXTERNAL_TOOL_TIMEOUT_MILLIS;
    }

    /**
     * 获取审批等待超时（固定默认 0，表示不限制）
     */
    public long getApprovalTimeoutMillis() {
        return DEFAULT_APPROVAL_TIMEOUT_MILLIS;
    }

    /**
     * 获取表单等待超时（固定默认 0，表示不限制）
     */
    public long getUserInputTimeoutMillis() {
        return DEFAULT_USER_INPUT_TIMEOUT_MILLIS;
    }

    /**
     * 获取挂起过期策略（固定默认 REJECT_RESUME）
     */
    public String getSuspensionExpirationStrategy() {
        return DEFAULT_SUSPENSION_EXPIRATION_STRATEGY;
    }

    /**
     * 获取本地工具结果上限（固定默认 0，表示不限制）
     */
    public long getToolResultMaxCharacters() {
        return DEFAULT_TOOL_RESULT_MAX_CHARACTERS;
    }

    /**
     * 获取外部工具结果上限（固定默认 0，表示不限制）
     */
    public long getExternalToolResultMaxCharacters() {
        return DEFAULT_EXTERNAL_TOOL_RESULT_MAX_CHARACTERS;
    }

    /**
     * 获取结果超限策略（固定默认 FAIL）
     */
    public String getToolResultOverflowStrategy() {
        return DEFAULT_TOOL_RESULT_OVERFLOW_STRATEGY;
    }

    /**
     * 获取工具执行模式（固定默认 SEQUENTIAL）
     */
    public String getToolExecutionMode() {
        return DEFAULT_TOOL_EXECUTION_MODE;
    }

    /**
     * 获取并行工具上限（固定默认 8）
     */
    public int getMaxParallelToolCalls() {
        return DEFAULT_MAX_PARALLEL_TOOL_CALLS;
    }

    /**
     * 获取并行失败策略（固定默认 FAIL_FAST）
     */
    public String getParallelFailureStrategy() {
        return DEFAULT_PARALLEL_FAILURE_STRATEGY;
    }

    /**
     * 获取工具中断消息模板（固定默认）
     */
    public String getInterruptedToolMessageTemplate() {
        return DEFAULT_INTERRUPTED_TOOL_MESSAGE_TEMPLATE;
    }

    /**
     * 获取 Turn 中断消息模板（固定默认）
     */
    public String getInterruptedTurnMessageTemplate() {
        return DEFAULT_INTERRUPTED_TURN_MESSAGE_TEMPLATE;
    }

    /**
     * 获取取消原因（固定默认）
     */
    public String getCancellationReason() {
        return DEFAULT_CANCELLATION_REASON;
    }

}
