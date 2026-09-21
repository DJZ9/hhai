package tech.aiflowy.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SQL 标识符工具类。
 * <p>
 * 表名、列名等位置无法使用 JDBC "?" 占位符，只能拼进 SQL 文本，因此这里只放行“纯标识符”形式的取值：
 * 按 "." 分段（最多 3 段，覆盖 table / db.table / db.schema.table），每段可用一对反引号包裹，
 * 去掉反引号后必须是 {@code [A-Za-z_$][A-Za-z0-9_$]*}，
 * 空白、引号、括号、逗号、分号、注释符、控制字符等一律拒绝。
 *
 * @author aiflowy
 */
public class SqlIdentifierUtil {

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
     * ORDER BY 表达式长度上限
     */
    private static final int MAX_ORDER_BY_LENGTH = 256;

    /**
     * 提示信息中回显取值的最大长度
     */
    private static final int MAX_DESCRIBE_LENGTH = 60;

    /**
     * 反引号
     */
    private static final char BACKTICK = '`';

    /**
     * 分段点分符
     */
    private static final String DOT = ".";

    /**
     * 每个分段的合法形式
     */
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private SqlIdentifierUtil() {
    }

    /**
     * 判断取值是否为合法的 SQL 标识符（表名、列名等）。
     *
     * @param value 待校验的取值
     * @return true 表示是合法的取值
     */
    public static boolean isValidIdentifier(String value) {
        if (value == null) {
            return false;
        }

        // 先拒绝控制字符（含 NUL、换行、制表符等）
        if (hasControlChar(value)) {
            return false;
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
     * 校验取值是否为合法标识符，不合法时抛出异常。
     *
     * @param fieldName 字段名，用于提示信息
     * @param value     待校验的取值
     */
    public static void checkIdentifier(String fieldName, String value) {
        if (!isValidIdentifier(value)) {
            throw new IllegalArgumentException(fieldName
                    + " 只能是合法的表名或列名（字母、数字、下划线或 $，最多 3 段点分），当前值：" + describe(value));
        }
    }

    /**
     * 把标识符转成反引号包裹的形式，例如 {@code db.tb_x -> `db`.`tb_x`}。
     * <p>
     * 反引号引用可以让标识符内的关键字、$ 等字符不参与 SQL 解析。
     *
     * @param value 已通过 {@link #isValidIdentifier(String)} 校验的取值
     * @return 反引号包裹后的标识符
     */
    public static String quoteIdentifier(String value) {
        if (!isValidIdentifier(value)) {
            throw new IllegalArgumentException("非法的 SQL 标识符（只能是字母、数字、下划线或 $，最多 3 段点分）："
                    + describe(value));
        }

        String[] segments = value.strip().split("\\.", -1);
        StringBuilder quoted = new StringBuilder(value.length() + segments.length * 2);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                quoted.append(DOT);
            }
            quoted.append(BACKTICK).append(stripBacktick(segments[i])).append(BACKTICK);
        }
        return quoted.toString();
    }

    /**
     * 判断取值是否为合法的 ORDER BY 表达式：列名，可带 asc/desc，多个用逗号分隔。
     *
     * @param value 待校验的取值
     * @return true 表示是合法的取值
     */
    public static boolean isValidOrderBy(String value) {
        if (value == null || hasControlChar(value)) {
            return false;
        }

        String orderBy = value.strip();
        if (orderBy.isEmpty() || orderBy.length() > MAX_ORDER_BY_LENGTH) {
            return false;
        }

        String[] items = orderBy.split(",", -1);
        for (String item : items) {
            if (!isValidOrderByItem(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验 ORDER BY 表达式，不合法时抛出异常。
     *
     * @param fieldName 字段名，用于提示信息
     * @param value     待校验的取值
     */
    public static void checkOrderBy(String fieldName, String value) {
        if (!isValidOrderBy(value)) {
            throw new IllegalArgumentException(fieldName
                    + " 只能是列名（可带 asc/desc，多个用逗号分隔），当前值：" + describe(value));
        }
    }

    /**
     * 把合法的 ORDER BY 表达式转成反引号引用列名的形式，例如 {@code sort_no asc -> `sort_no` asc}。
     *
     * @param value 已通过 {@link #isValidOrderBy(String)} 校验的取值
     * @return 反引号引用后的 ORDER BY 表达式
     */
    public static String quoteOrderBy(String value) {
        if (!isValidOrderBy(value)) {
            throw new IllegalArgumentException("非法的 ORDER BY 配置（只能是列名，可带 asc/desc，多个用逗号分隔）："
                    + describe(value));
        }

        String[] items = value.strip().split(",", -1);
        StringBuilder quoted = new StringBuilder(value.length() + 16);
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                quoted.append(", ");
            }
            String[] tokens = items[i].strip().split("\\s+");
            quoted.append(quoteIdentifier(tokens[0]));
            if (tokens.length == 2) {
                quoted.append(' ').append(tokens[1].toLowerCase(Locale.ROOT));
            }
        }
        return quoted.toString();
    }

    private static boolean isValidOrderByItem(String item) {
        String[] tokens = item.strip().split("\\s+");
        if (tokens.length == 0 || tokens.length > 2) {
            return false;
        }
        if (!isValidIdentifier(tokens[0])) {
            return false;
        }
        if (tokens.length == 1) {
            return true;
        }
        return "asc".equalsIgnoreCase(tokens[1]) || "desc".equalsIgnoreCase(tokens[1]);
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
            name = stripBacktick(segment);
            if (name.isEmpty() || name.indexOf(BACKTICK) >= 0) {
                return false;
            }
        }

        if (name.length() > MAX_SEGMENT_LENGTH) {
            return false;
        }
        return SEGMENT_PATTERN.matcher(name).matches();
    }

    private static String stripBacktick(String segment) {
        if (segment.length() >= 2 && segment.charAt(0) == BACKTICK && segment.charAt(segment.length() - 1) == BACKTICK) {
            return segment.substring(1, segment.length() - 1);
        }
        return segment;
    }

    private static boolean hasControlChar(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成用于日志与提示的取值描述：换行/制表符扁平化并截断。
     */
    private static String describe(String value) {
        if (value == null) {
            return "(空)";
        }
        String flattened = value.replaceAll("[\\r\\n\\t]", " ").strip();
        if (flattened.isEmpty()) {
            return "(空)";
        }
        if (flattened.length() > MAX_DESCRIBE_LENGTH) {
            return flattened.substring(0, MAX_DESCRIBE_LENGTH) + "...";
        }
        return flattened;
    }
}
