package tech.aiflowy.ai.utils;

import tech.aiflowy.common.web.exceptions.BusinessException;

import java.util.regex.Pattern;

/**
 * 工作流 SqlNode 的标识符处理工具。
 * <p>
 * SqlNode 支持 {@code {{param}}} 语法把运行时变量拼入 SQL，用于表名、列名等无法使用 JDBC "?" 占位符
 * 的位置。这类取值只放行“纯标识符”形式：按 "." 分段（最多 3 段，覆盖 db.table / db.schema.table），
 * 每段可用一对反引号包裹，去掉反引号后必须是 {@code [A-Za-z_$][A-Za-z0-9_$]*}，
 * 空白、引号、括号、逗号、分号、注释符等一律拒绝。
 *
 * @author aiflowy
 */
public class SqlNodeIdentifierUtil {

    /**
     * 标识符总长度上限
     */
    private static final int MAX_IDENTIFIER_LENGTH = 128;

    /**
     * 单个标识符分段长度上限
     */
    private static final int MAX_SEGMENT_LENGTH = 64;

    /**
     * 标识符最大分段数
     */
    private static final int MAX_SEGMENT_COUNT = 3;

    /**
     * 反引号
     */
    private static final char BACKTICK = '`';

    private static final Pattern SEGMENT_PATTERN = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private SqlNodeIdentifierUtil() {
    }

    /**
     * 判断取值是否为合法的 SQL 标识符（表名、列名等）。
     *
     * @param value 运行时变量的值
     * @return true 表示是合法的取值
     */
    public static boolean isValidIdentifier(String value) {
        if (value == null) {
            return false;
        }

        // 先拒绝控制字符（含 NUL、换行、制表符等）
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return false;
            }
        }

        String identifier = value.strip();
        if (identifier.isEmpty() || identifier.length() > MAX_IDENTIFIER_LENGTH) {
            return false;
        }

        String[] segments = identifier.split("\\.", -1);
        if (segments.length == 0 || segments.length > MAX_SEGMENT_COUNT) {
            return false;
        }
        for (String segment : segments) {
            if (!isValidSegment(segment)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验取值是否为合法标识符，不合法时抛出业务异常。
     *
     * @param paramName 参数名，用于提示信息
     * @param value     运行时变量的值
     */
    public static void checkIdentifier(String paramName, String value) {
        if (!isValidIdentifier(value)) {
            throw new BusinessException("SQL 参数 [" + paramName + "] 只能是合法的表名或列名；条件值请改用 {{?" + paramName + "}} 占位符");
        }
    }

    private static boolean isValidSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            return false;
        }

        String name = segment;
        boolean hasBacktick = segment.indexOf(BACKTICK) >= 0;
        if (hasBacktick) {
            // 反引号必须成对包裹整个分段，且内部不允许再出现反引号
            if (segment.length() < 3
                    || segment.charAt(0) != BACKTICK
                    || segment.charAt(segment.length() - 1) != BACKTICK) {
                return false;
            }
            name = segment.substring(1, segment.length() - 1);
            if (name.isEmpty() || name.indexOf(BACKTICK) >= 0) {
                return false;
            }
        }

        if (name.length() > MAX_SEGMENT_LENGTH) {
            return false;
        }
        return SEGMENT_PATTERN.matcher(name).matches();
    }
}
