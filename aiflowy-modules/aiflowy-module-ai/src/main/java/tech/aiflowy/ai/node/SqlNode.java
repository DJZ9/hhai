package tech.aiflowy.ai.node;

import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.BaseNode;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import tech.aiflowy.ai.utils.SqlNodeIdentifierUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * SQL查询节点
 *
 * @author tao
 * @date 2025-05-21
 */
public class SqlNode extends BaseNode {

    private static final Logger logger = LoggerFactory.getLogger(SqlNode.class);

    /**
     * 匹配 {{?param}}：可用 JDBC "?" 参数化的值占位符
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{\\{\\?([^}]+)}}");

    /**
     * 匹配 {{param}}：标识符占位符（表名、列名等），值必须是合法标识符
     */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\{\\{([^}?][^}]*)}}");

    /**
     * 拒绝日志中参数值的最大长度
     */
    private static final int MAX_LOGGED_VALUE_LENGTH = 200;

    private String sql;

    public SqlNode() {
    }

    public SqlNode(String sql) {
        this.sql = sql;
    }

    @Override
    public Map<String, Object> execute(Chain chain) {

        Map<String, Object> map = chain.getState().resolveParameters(this);
        Map<String, Object> res = new HashMap<>();

        Map<String, Object> formatSqlMap = formatSql(sql, map, getId());
        String formatSql = (String) formatSqlMap.get("replacedSql");

        validateSelectStatement(formatSql);

        List<String> paramNames = (List<String>) formatSqlMap.get("paramNames");

        List<Object> paramValues = new ArrayList<>();
        paramNames.forEach(paramName -> {
            Object o = map.get(paramName);
            paramValues.add(o);
        });

        List<Row> rows = Db.selectListBySql(formatSql, paramValues.toArray());

        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }

        res.put("queryData", rows);
        return res;
    }

    /**
     * 将 SQL 模板渲染为可执行的 SQL。
     * <p>
     * {{?param}} 会被替换为 JDBC 的 "?"，值通过参数绑定传入；
     * {{param}} 仅用于表名、列名等无法参数化的位置，取值必须通过 {@link SqlNodeIdentifierUtil} 校验，
     * 否则拒绝执行。
     *
     * @param rawSql   作者编写的 SQL 模板
     * @param paramMap 运行时解析出的参数
     * @return replacedSql：渲染后的 SQL；paramNames：{{?param}} 对应的参数名（顺序即 "?" 的顺序）
     */
    static Map<String, Object> formatSql(String rawSql, Map<String, Object> paramMap) {
        return formatSql(rawSql, paramMap, null);
    }

    /**
     * 将 SQL 模板渲染为可执行的 SQL，并携带节点 ID 用于拒绝日志。
     *
     * @param rawSql   作者编写的 SQL 模板
     * @param paramMap 运行时解析出的参数
     * @param nodeId   节点 ID，仅用于日志
     * @return replacedSql：渲染后的 SQL；paramNames：{{?param}} 对应的参数名（顺序即 "?" 的顺序）
     */
    static Map<String, Object> formatSql(String rawSql, Map<String, Object> paramMap, String nodeId) {

        if (!StringUtils.hasLength(rawSql)) {
            logger.error("sql解析报错：sql为空");
            throw new BusinessException("sql 不能为空！");
        }

        // 模板规范化只作用于作者编写的模板，绝不作用于运行时替换后的内容
        String normalizedSql = normalizeTemplate(rawSql);

        List<String> paramNames = new ArrayList<>();
        StringBuffer sqlBuffer = new StringBuffer();

        // 替换 {{?param}} -> ?
        Matcher paramMatcher = PARAM_PATTERN.matcher(normalizedSql);
        while (paramMatcher.find()) {
            String paramName = paramMatcher.group(1).trim();
            paramNames.add(paramName);
            paramMatcher.appendReplacement(sqlBuffer, "?");
        }
        paramMatcher.appendTail(sqlBuffer);
        String intermediateSql = sqlBuffer.toString();

        Map<String, Object> params = paramMap == null ? Collections.emptyMap() : paramMap;

        // 替换 {{param}} -> 合法标识符（表名、列名等）
        sqlBuffer = new StringBuffer(); // 清空 buffer 重新处理
        Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher(intermediateSql);
        while (identifierMatcher.find()) {
            String key = identifierMatcher.group(1).trim();
            Object value = params.get(key);
            if (value == null) {
                logger.warn("未找到参数：{}", key);
                throw new BusinessException("sql解析失败，请确保sql语法正确！");
            }

            String identifier = value.toString().strip();
            if (!SqlNodeIdentifierUtil.isValidIdentifier(identifier)) {
                logger.warn("检测到非法的 SQL 标识符参数，已拒绝执行：nodeId={}, param={}, value={}",
                        nodeId, key, truncateForLog(identifier));
                throw new BusinessException("SQL 参数 [" + key + "] 只能是合法的表名或列名；条件值请改用 {{?" + key + "}} 占位符");
            }

            identifierMatcher.appendReplacement(sqlBuffer, Matcher.quoteReplacement(identifier));
        }
        identifierMatcher.appendTail(sqlBuffer);

        String finalSql = sqlBuffer.toString().trim();

        logger.info("Final SQL: {}", finalSql);
        logger.info("Param names: {}", paramNames);

        Map<String, Object> result = new HashMap<>();
        result.put("replacedSql", finalSql);
        result.put("paramNames", paramNames);
        return result;
    }

    /**
     * 校验最终 SQL：必须且只能是一条查询语句。
     * <p>
     * 使用 parseStatements 而非 parse：后者只取第一条语句。
     *
     * @param sql 渲染后的 SQL
     */
    static void validateSelectStatement(String sql) {
        if (!StringUtils.hasLength(sql)) {
            logger.error("sql解析报错：sql为空");
            throw new BusinessException("sql 不能为空！");
        }

        Statements statements;
        try {
            statements = CCJSqlParserUtil.parseStatements(sql);
        } catch (JSQLParserException e) {
            logger.error("sql 解析报错：", e);
            throw new BusinessException("SQL解析失败，请确认SQL语法无误");
        }

        List<Statement> statementList = statements.getStatements();
        if (statementList == null || statementList.size() != 1) {
            logger.error("sql 解析报错：仅支持单条查询语句，实际解析出 {} 条", statementList == null ? 0 : statementList.size());
            throw new BusinessException("仅支持单条查询语句！");
        }

        Statement statement = statementList.get(0);
        if (!(statement instanceof Select)) {
            logger.error("sql 解析报错：statement instanceof Select 结果为false");
            throw new BusinessException("仅支持查询语句！");
        }
    }

    /**
     * 清理模板末尾的分号与中文引号（仅作用于作者编写的模板）。
     */
    private static String normalizeTemplate(String rawSql) {
        String normalized = rawSql.trim();
        if (normalized.endsWith(";") || normalized.endsWith("；")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.replace("“", "\"").replace("”", "\"");
    }

    /**
     * 日志中的参数值做换行/制表符扁平化与截断，避免日志伪造与超长日志。
     */
    private static String truncateForLog(String value) {
        if (value == null) {
            return null;
        }
        String flattened = value.replaceAll("[\\r\\n\\t]", " ");
        return flattened.length() > MAX_LOGGED_VALUE_LENGTH
                ? flattened.substring(0, MAX_LOGGED_VALUE_LENGTH) + "..."
                : flattened;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
