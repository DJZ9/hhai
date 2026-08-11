package tech.aiflowy.ai.entity;

import com.mybatisflex.annotation.RelationManyToMany;
import tech.aiflowy.ai.entity.base.BotBase;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.common.util.MapUtil;
import tech.aiflowy.system.entity.SysDept;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实体类。
 *
 * @author michael
 * @since 2024-08-23
 */

@Table("tb_bot")
public class Bot extends BotBase {

    /**
     * 可见部门ID列表（非数据库字段，用于接收前端参数）
     */
    private List<BigInteger> scopeOfAuthorityDeptIds;

    /**
     * 部门关联列表（关联查询用）
     */
    @RelationManyToMany(
            joinTable = "tb_bot_dept_mapping",
            joinSelfColumn = "bot_id",
            joinTargetColumn = "dept_id",
            targetField = "id"
    )
    private List<SysDept> depts;

    public List<BigInteger> getScopeOfAuthorityDeptIds() {
        return scopeOfAuthorityDeptIds;
    }

    public void setScopeOfAuthorityDeptIds(List<BigInteger> scopeOfAuthorityDeptIds) {
        this.scopeOfAuthorityDeptIds = scopeOfAuthorityDeptIds;
    }

    public List<SysDept> getDepts() {
        return depts;
    }

    public void setDepts(List<SysDept> depts) {
        this.depts = depts;
        // 同步提取部门ID列表，方便前端使用
        if (depts != null) {
            this.scopeOfAuthorityDeptIds = depts.stream()
                    .map(SysDept::getId)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取部门ID列表（从depts中提取）
     */
    public List<BigInteger> getDeptIds() {
        if (depts == null) {
            return null;
        }
        return depts.stream()
                .map(SysDept::getId)
                .collect(Collectors.toList());
    }

    public static final String KEY_ENABLE_SENSITIVE_FILTER = "enableSensitiveFilter";
    public static final String KEY_ENABLE_CLOUD_FILTER = "enableCloudFilter";
    public static final String KEY_SYSTEM_PROMPT = "systemPrompt";
    public static final String KEY_MAX_MESSAGE_COUNT = "maxMessageCount";
    public static final String KEY_ENABLE_DEEP_THINKING = "enableDeepThinking";
    public static final String KEY_EXPOSE_DEEP_THINKING = "exposeDeepThinking";

    // 对话历史压缩配置相关常量（仅支持AI摘要策略）
    // 使用 KEY_MAX_MESSAGE_COUNT 作为最大历史轮数
    public static final String KEY_COMPRESSION_ENABLED = "compressionEnabled";
    public static final String KEY_COMPRESSION_ROUNDS = "compressionRounds";
    public static final String KEY_COMPRESSION_MODEL_ID = "compressionModelId";
    public static final String KEY_COMPRESSION_TOKEN_THRESHOLD = "compressionTokenThreshold";

    // 附件功能配置相关常量
    public static final String KEY_ATTACHMENT_ENABLED = "attachmentEnabled";
    public static final String KEY_VL_MODEL_ID = "vlModelId";

    // 提示词变量
    public static final String KEY_OPTIONS_PROMPTWORDUSERVARIABLE = "promptWordUserVariable";
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
     * 是否启用对话历史压缩
     */
    public boolean isCompressionEnabled() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return false;
        }
        Object o = options.get(KEY_COMPRESSION_ENABLED);
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
     * 获取最大历史对话轮数（使用maxMessageCount），默认10轮
     * 超过这个轮数后，会触发压缩
     */
    public int getMaxHistoryRounds() {
        Map<String, Object> options = getModelOptions();
        if (options == null) {
            return 5;
        }
        Integer maxMessageCount = MapUtil.getInteger(options, Bot.KEY_MAX_MESSAGE_COUNT);

        if (maxMessageCount != null) {
            return maxMessageCount;
        }

        return 5;
    }

    /**
     * 获取压缩轮数，默认5轮
     * 当对话超过最大历史轮数时，压缩前面多少轮
     */
    public int getCompressionRounds() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return 5;
        }
        Object o = options.get(KEY_COMPRESSION_ROUNDS);
        if (o == null) {
            return 5;
        }
        if (o instanceof Integer) {
            return (Integer) o;
        }
        if (o instanceof String) {
            try {
                return Integer.parseInt((String) o);
            } catch (NumberFormatException e) {
                return 5;
            }
        }
        return 5;
    }

    /**
     * 获取压缩模型ID，用于AI摘要生成
     */
    public BigInteger getCompressionModelId() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return null;
        }
        Object o = options.get(KEY_COMPRESSION_MODEL_ID);
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            String str = (String) o;
            if (str.isEmpty()) {
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
     * 获取压缩Token阈值，默认500
     * 当AI回复内容超过这个token数时，才进行压缩
     */
    public int getCompressionTokenThreshold() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return 500;
        }
        Object o = options.get(KEY_COMPRESSION_TOKEN_THRESHOLD);
        if (o == null) {
            return 500;
        }
        if (o instanceof Integer) {
            return (Integer) o;
        }
        if (o instanceof String) {
            try {
                return Integer.parseInt((String) o);
            } catch (NumberFormatException e) {
                return 500;
            }
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return 500;
    }

    /**
     * 是否启用云端AI过滤
     */
    public boolean isCloudFilterEnabled() {
        Map<String, Object> options = getOptions();
        if (options == null) {
            return false;
        }
        Object o = options.get(KEY_ENABLE_CLOUD_FILTER);
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
