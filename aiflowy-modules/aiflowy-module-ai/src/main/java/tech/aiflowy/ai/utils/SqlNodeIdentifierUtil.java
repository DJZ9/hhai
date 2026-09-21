package tech.aiflowy.ai.utils;

import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.common.util.SqlIdentifierUtil;

/**
 * 工作流 SqlNode 的标识符处理入口。
 * <p>
 * 校验规则统一由 {@link SqlIdentifierUtil} 实现，其它需要拼接标识符的场景复用同一套规则，
 * 避免各处处理口径不一致。这里只保留 SqlNode 专属的报错文案（提示改用 {{?param}} 占位符）。
 *
 * @author aiflowy
 */
public class SqlNodeIdentifierUtil {

    private SqlNodeIdentifierUtil() {
    }

    /**
     * 判断取值是否为合法的 SQL 标识符（表名、列名等）。
     *
     * @param value 运行时变量的值
     * @return true 表示是合法的取值
     */
    public static boolean isValidIdentifier(String value) {
        return SqlIdentifierUtil.isValidIdentifier(value);
    }

    /**
     * 校验取值是否为合法标识符，不合法时抛出业务异常。
     *
     * @param paramName 参数名，用于提示信息
     * @param value     运行时变量的值
     */
    public static void checkIdentifier(String paramName, String value) {
        if (!SqlIdentifierUtil.isValidIdentifier(value)) {
            throw new BusinessException("SQL 参数 [" + paramName + "] 只能是合法的表名或列名；条件值请改用 {{?" + paramName + "}} 占位符");
        }
    }
}
