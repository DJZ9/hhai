package tech.aiflowy.datacenter.utils;

import tech.aiflowy.common.web.exceptions.BusinessException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SqlInjectionUtils {

    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
            "select", "insert", "update", "delete", "drop", "alter", "create",
            "table", "where", "from", "join", "union", "truncate", "execute",
            "grant", "revoke", "commit", "rollback"
    ));

    /**
     * 校验字段或表名
     */
    public static String checkIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new BusinessException("标识符不能为空");
        }
        if (identifier.length() > 64) {
            throw new BusinessException("标识符过长");
        }
        // 检查是否只包含字母、数字和下划线
        if (!identifier.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException("只允许字母、数字和下划线");
        }
        if (isSqlKeyword(identifier)) {
            throw new BusinessException("非法字符");
        }
        return identifier;
    }

   /**
     * 校验注释
     * 允许的字符包括以下 Unicode 类别或符号：
     * \p{L}：任何语言的字母（包括中文、英文、日文等）。
     * \p{N}：任何数字（包括阿拉伯数字 0-9 或其他语言的数字符号）。
     * \p{Zs}：空白分隔符（如空格，但不包括换行符、制表符等）。
     * 标点符号：. , - : ? !（基础标点）。
     * 非法字符将被替换为下划线 _
     */
    public static String checkComment(String comment) {
        if (comment == null || comment.isEmpty()) {
            return "";
        }
        if (comment.length() > 255) {
            throw new BusinessException("注释过长");
        }
        // 替换非法字符为下划线
        comment = comment.replaceAll("[^\\p{L}\\p{N}\\p{Zs}\\.\\,\\-\\:\\?\\!]", "_");
        // 将连续的多个下划线替换为单个下划线
        comment = comment.replaceAll("_+", "_");
        // 去除首尾下划线
        comment = comment.replaceAll("^_+|_+$", "");
        return comment;
    }

    // 检查是否是数据库关键字
    public static boolean isSqlKeyword(String word) {
        return SQL_KEYWORDS.contains(word.toLowerCase());
    }
}
