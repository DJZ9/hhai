package tech.aiflowy.datacenter.interceptor;

import com.agentsflex.core.model.chat.ChatContext;
import com.agentsflex.core.model.chat.ChatContextHolder;
import com.agentsflex.text2sql.core.SqlExecuteContext;
import com.agentsflex.text2sql.core.SqlInterceptor;
import com.agentsflex.text2sql.core.SqlInvocation;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.aiflowy.common.constant.Constants;
import tech.aiflowy.datacenter.entity.*;
import tech.aiflowy.datacenter.service.*;
import tech.aiflowy.datacenter.utils.SqlInfo;
import tech.aiflowy.datacenter.utils.SqlTemplate;
import tech.aiflowy.system.entity.SysAccount;
import tech.aiflowy.system.entity.SysDept;
import tech.aiflowy.system.entity.SysRole;
import tech.aiflowy.system.service.SysDeptService;
import tech.aiflowy.system.service.SysRoleService;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字段权限 SQL 拦截器
 * 根据用户部门权限过滤 SQL 查询中的字段
 * 同时支持数据中枢表和数据源字段
 */
@Component
public class FieldPermissionSqlInterceptor implements SqlInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FieldPermissionSqlInterceptor.class);

    @Resource
    private DatacenterTableFieldService tableFieldService;

    @Resource
    private DatacenterSourceFieldService sourceFieldService;

    @Resource
    private DatacenterTableService datacenterTableService;

    @Resource
    private DatacenterSourceTableService datacenterSourceTableService;

    @Resource
    private DatacenterSourceService datacenterSourceService;

    @Resource
    private SysDeptService sysDeptService;

    @Resource
    private SysRoleService sysRoleService;

    @Override
    public Object intercept(SqlInvocation invocation) throws Exception {
        SqlExecuteContext ctx = invocation.getContext();
        String dataSourceName = ctx.getDataSource().getName();
        String sql = ctx.getSql();

        // 从 ChatContext 获取当前用户信息
        ChatContext chatContext = ChatContextHolder.currentContext();
        if (chatContext == null) {
            return invocation.proceed();
        }

        Map<String, Object> attributes = chatContext.getAttributes();
        Object loginAccountObj = attributes.get(Constants.LOGIN_USER_KEY);
        if (!(loginAccountObj instanceof SysAccount)) {
            return invocation.proceed();
        }

        SysAccount loginAccount = (SysAccount) loginAccountObj;
        BigInteger userDeptId = loginAccount.getDeptId();
        if (userDeptId == null) {
            return invocation.proceed();
        }

        // ==================== 数据源部门权限检查 ====================
        // 如果不是数据中枢(datacenter),则检查数据源的部门访问权限
        if (!"datacenter".equalsIgnoreCase(dataSourceName)) {
            // 尝试从 DataSource 对象中获取更多信息进行精确查询
            Object dataSourceObj = ctx.getDataSource();
            DatacenterSource datacenterSource = getDatacenterSource(dataSourceObj, dataSourceName);
            
            if (datacenterSource != null) {
                // 检查用户是否有权限访问该数据源
                if (!datacenterSourceService.hasAuthority(loginAccount.getId(), datacenterSource)) {
                    return "Error: 您没有权限访问数据源 \"" + dataSourceName + "\"。请联系管理员申请访问权限。";
                }
            }
        }
        // ==================== 数据源部门权限检查结束 ====================

        // 解析 SQL，检测是否有无权限字段
        SqlCheckResult checkResult = checkSqlPermissions(sql, userDeptId, dataSourceName);

        // 如果 AI 明确指定了无权限字段（非 SELECT *），直接返回错误
        if (!checkResult.isSelectAll() && !checkResult.getNoPermissionFields().isEmpty()) {
            String errorMsg = "Error: 您没有权限访问以下字段："
                    + String.join(", ", checkResult.getNoPermissionFields())
                    + "。请修改 SQL，排除这些字段后重试。";
            return errorMsg;
        }

        // 如果是 SELECT * 或没有无权限字段，正常执行（SELECT * 时默默过滤）
        if (checkResult.isSelectAll() && !checkResult.getFilteredFields().isEmpty()) {
            ctx.setSql(checkResult.getFilteredSql());
        }

        // 注入自定义 SQL 条件
        String currentSql = ctx.getSql();
        List<Object> currentParams = ctx.getParameters();
        SqlInfo sqlInfo = injectCustomConditions(currentSql, currentParams, dataSourceName, loginAccount);
        if (sqlInfo != null) {
            ctx.setSql(sqlInfo.getSql());
            if (sqlInfo.getParameters() != null && !sqlInfo.getParameters().isEmpty()) {
                ctx.setParameters(sqlInfo.getParameters());
            }
        }

        Object result = invocation.proceed();

        // SELECT * 的情况下，在结果中附加说明
        if (checkResult.isSelectAll() && !checkResult.getFilteredFields().isEmpty() && result instanceof String) {
            String resultStr = (String) result;
            if (!resultStr.startsWith("Error:")) {
                result = resultStr + "\n\n[系统提示：以下字段因您当前部门权限被隐藏，未包含在查询结果中："
                        + String.join(", ", checkResult.getFilteredFields()) + "]";
            }
        }

        return result;
    }

    /**
     * 检查 SQL 权限并过滤字段
     */
    private SqlCheckResult checkSqlPermissions(String sql, BigInteger userDeptId, String dataSourceName) {
        SqlCheckResult result = new SqlCheckResult(sql);
        if (sql == null || sql.trim().isEmpty()) {
            return result;
        }

        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                checkSelectPermissions(select, userDeptId, dataSourceName, result);
                result.setFilteredSql(select.toString());
            }
        } catch (Exception e) {
            log.error("SQL 字段过滤失败: {}", sql, e);
        }
        return result;
    }

    private void checkSelectPermissions(Select select, BigInteger userDeptId, String dataSourceName, SqlCheckResult result) {
        if (select instanceof PlainSelect) {
            checkPlainSelectPermissions((PlainSelect) select, userDeptId, dataSourceName, result);
        } else if (select instanceof SetOperationList) {
            SetOperationList setOpList = (SetOperationList) select;
            for (Select subSelect : setOpList.getSelects()) {
                checkSelectPermissions(subSelect, userDeptId, dataSourceName, result);
            }
        }
    }

    private void checkPlainSelectPermissions(PlainSelect plainSelect, BigInteger userDeptId, String dataSourceName, SqlCheckResult result) {
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof net.sf.jsqlparser.schema.Table) {
            net.sf.jsqlparser.schema.Table table = (net.sf.jsqlparser.schema.Table) fromItem;
            String tableName = table.getName().replace("`", "").trim();

            Map<String, List<BigInteger>> fieldPermissions = getFieldPermissions(tableName, dataSourceName);

            if (!fieldPermissions.isEmpty()) {
                Set<String> fieldsToRemove = new HashSet<>();
                for (Map.Entry<String, List<BigInteger>> entry : fieldPermissions.entrySet()) {
                    List<BigInteger> allowedDeptIds = entry.getValue();
                    if (!allowedDeptIds.contains(userDeptId)) {
                        fieldsToRemove.add(entry.getKey().trim().toLowerCase());
                    }
                }

                if (!fieldsToRemove.isEmpty()) {
                    List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
                    if (selectItems != null) {
                        boolean hasSelectAll = false;
                        for (SelectItem<?> item : selectItems) {
                            if (item.getExpression() instanceof AllColumns
                                    || item.getExpression() instanceof AllTableColumns) {
                                hasSelectAll = true;
                                break;
                            }
                        }
                        result.setSelectAll(hasSelectAll);

                        if (hasSelectAll) {
                            // SELECT * 时，展开为具体字段
                            List<SelectItem<?>> expandedItems = new ArrayList<>();
                            List<String> allFieldNames = getAllFieldNames(tableName, dataSourceName);
                            for (String fieldName : allFieldNames) {
                                String normalizedFieldName = fieldName.toLowerCase();
                                if (fieldsToRemove.contains(normalizedFieldName)) {
                                    result.addFilteredField(fieldName);
                                } else {
                                    Column column = new Column();
                                    column.setColumnName(fieldName);
                                    SelectItem<?> selectItem = new SelectItem<>(column);
                                    expandedItems.add(selectItem);
                                }
                            }
                            if (!expandedItems.isEmpty()) {
                                plainSelect.setSelectItems(expandedItems);
                            }
                        } else {
                            // 非 SELECT *，逐个检查字段
                            List<SelectItem<?>> filteredItems = new ArrayList<>();
                            for (SelectItem<?> item : selectItems) {
                                String fieldName = extractFieldName(item);
                                if (fieldName == null) {
                                    filteredItems.add(item);
                                    continue;
                                }

                                String normalizedFieldName = fieldName.trim().toLowerCase();
                                if (fieldsToRemove.contains(normalizedFieldName)) {
                                    result.addNoPermissionField(fieldName);
                                } else {
                                    filteredItems.add(item);
                                }
                            }
                            plainSelect.setSelectItems(filteredItems);
                        }
                    }
                }
            }
        }

        // 递归处理子查询
        if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) fromItem;
            Select subSelectBody = parenthesedSelect.getSelect();
            if (subSelectBody != null) {
                checkSelectPermissions(subSelectBody, userDeptId, dataSourceName, result);
            }
        }

        // 处理 JOIN
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                if (rightItem instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) rightItem;
                    Select subSelectBody = parenthesedSelect.getSelect();
                    if (subSelectBody != null) {
                        checkSelectPermissions(subSelectBody, userDeptId, dataSourceName, result);
                    }
                }
            }
        }
    }

    /**
     * 获取表的字段权限配置
     * 根据数据源名称判断查询哪个 service
     */
    private Map<String, List<BigInteger>> getFieldPermissions(String tableName, String dataSourceName) {
        Map<String, List<BigInteger>> permissions = new HashMap<>();

        try {
            if ("datacenter".equalsIgnoreCase(dataSourceName)) {
                // 数据中枢表字段
                log.debug("正在查询数据中枢表 {} 的字段权限配置", tableName);
                List<DatacenterTableField> fields = tableFieldService.getFieldsByTableName(tableName);
                log.debug("数据中枢表 {} 查询到 {} 个字段", tableName, fields != null ? fields.size() : 0);

                if (fields == null || fields.isEmpty()) {
                    return permissions;
                }

                for (DatacenterTableField field : fields) {
                    Map<String, Object> options = field.getOptions();
                    if (options != null && !options.isEmpty()) {
                        List<BigInteger> deptIds = parseDeptIds(options);
                        if (!deptIds.isEmpty()) {
                            String normalizedFieldName = field.getFieldName().trim().toLowerCase();
                            permissions.put(normalizedFieldName, deptIds);
                            log.debug("字段 {} (标准化: {}) 配置了部门权限: {}", field.getFieldName(), normalizedFieldName, deptIds);
                        }
                    }
                }
            } else {
                // 数据源字段
                log.debug("正在查询数据源表 {} 的字段权限配置", tableName);
                List<DatacenterSourceField> fields = sourceFieldService.getFieldsByTableName(tableName);
                log.debug("数据源表 {} 查询到 {} 个字段", tableName, fields != null ? fields.size() : 0);

                if (fields == null || fields.isEmpty()) {
                    return permissions;
                }

                for (DatacenterSourceField field : fields) {
                    Map<String, Object> options = field.getOptions();
                    if (options != null && !options.isEmpty()) {
                        List<BigInteger> deptIds = parseDeptIds(options);
                        if (!deptIds.isEmpty()) {
                            String normalizedFieldName = field.getName().trim().toLowerCase();
                            permissions.put(normalizedFieldName, deptIds);
                            log.debug("数据源字段 {} 配置了部门权限: {}", field.getName(), deptIds);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取字段权限配置失败，tableName={}", tableName, e);
        }

        return permissions;
    }

    /**
     * 获取表的所有字段名（用于 SELECT * 展开）
     */
    private List<String> getAllFieldNames(String tableName, String dataSourceName) {
        List<String> fieldNames = new ArrayList<>();
        try {
            if ("datacenter".equalsIgnoreCase(dataSourceName)) {
                List<DatacenterTableField> fields = tableFieldService.getFieldsByTableName(tableName);
                if (fields != null) {
                    for (DatacenterTableField field : fields) {
                        fieldNames.add(field.getFieldName().replace("`", "").trim());
                    }
                }
            } else {
                List<DatacenterSourceField> fields = sourceFieldService.getFieldsByTableName(tableName);
                if (fields != null) {
                    for (DatacenterSourceField field : fields) {
                        fieldNames.add(field.getName().replace("`", "").trim());
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取表字段列表失败，tableName={}", tableName, e);
        }
        return fieldNames;
    }

    /**
     * SQL 权限检查结果
     */
    private static class SqlCheckResult {
        private final String originalSql;
        private String filteredSql;
        private boolean selectAll = false;
        private final Set<String> noPermissionFields = new HashSet<>();
        private final Set<String> filteredFields = new HashSet<>();

        public SqlCheckResult(String originalSql) {
            this.originalSql = originalSql;
            this.filteredSql = originalSql;
        }

        public String getFilteredSql() { return filteredSql; }
        public void setFilteredSql(String filteredSql) { this.filteredSql = filteredSql; }
        public boolean isSelectAll() { return selectAll; }
        public void setSelectAll(boolean selectAll) { this.selectAll = selectAll; }
        public Set<String> getNoPermissionFields() { return noPermissionFields; }
        public void addNoPermissionField(String field) { this.noPermissionFields.add(field); }
        public Set<String> getFilteredFields() { return filteredFields; }
        public void addFilteredField(String field) { this.filteredFields.add(field); }
    }

    /**
     * 解析 options 中的 deptIds
     */
    private List<BigInteger> parseDeptIds(Map<String, Object> options) {
        List<BigInteger> deptIds = new ArrayList<>();
        try {
            if (options != null && options.containsKey("deptIds")) {
                Object deptIdsObj = options.get("deptIds");
                if (deptIdsObj instanceof List) {
                    List<?> ids = (List<?>) deptIdsObj;
                    for (Object id : ids) {
                        BigInteger parsedId = parseIdToBigInteger(id);
                        if (parsedId != null) {
                            deptIds.add(parsedId);
                        }
                    }
                } else if (deptIdsObj instanceof com.alibaba.fastjson2.JSONArray) {
                    com.alibaba.fastjson2.JSONArray jsonArray = (com.alibaba.fastjson2.JSONArray) deptIdsObj;
                    for (int i = 0; i < jsonArray.size(); i++) {
                        BigInteger parsedId = parseIdToBigInteger(jsonArray.get(i));
                        if (parsedId != null) {
                            deptIds.add(parsedId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析 deptIds 失败，options={}", options, e);
        }
        return deptIds;
    }

    /**
     * 将各种类型的 ID 转换为 BigInteger
     */
    private BigInteger parseIdToBigInteger(Object id) {
        if (id == null) {
            return null;
        }
        if (id instanceof BigInteger) {
            return (BigInteger) id;
        }
        if (id instanceof Number) {
            return BigInteger.valueOf(((Number) id).longValue());
        }
        if (id instanceof String) {
            return new BigInteger((String) id);
        }
        return new BigInteger(id.toString());
    }

    /**
     * 从 SelectItem 中提取字段名（去除反引号）
     */
    private String extractFieldName(SelectItem<?> item) {
        if (item.getExpression() instanceof Column) {
            Column column = (Column) item.getExpression();
            return column.getColumnName().replace("`", "").trim();
        }
        return null;
    }

    // ==================== 自定义 SQL 条件注入 ====================

    /**
     * 注入自定义 SQL 条件到 WHERE 子句
     * 返回 SqlInfo 包含处理后的 SQL 和参数列表
     */
    private SqlInfo injectCustomConditions(String sql, List<Object> currentParams, String dataSourceName, SysAccount loginAccount) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                return null;
            }
            Select select = (Select) statement;
            if (!(select instanceof PlainSelect)) {
                return null;
            }
            PlainSelect plainSelect = (PlainSelect) select;
            FromItem fromItem = plainSelect.getFromItem();
            if (!(fromItem instanceof net.sf.jsqlparser.schema.Table)) {
                return null;
            }
            net.sf.jsqlparser.schema.Table table = (net.sf.jsqlparser.schema.Table) fromItem;
            String tableName = table.getName().replace("`", "").trim();

            List<String> whereClauses = getCustomWhereClauses(tableName, dataSourceName);
            if (whereClauses.isEmpty()) {
                return null;
            }

            // 合并原 SQL 的参数和自定义条件的参数
            List<Object> parameters = new ArrayList<>();
            if (currentParams != null) {
                parameters.addAll(currentParams);
            }

            List<Expression> expressions = new ArrayList<>();
            for (String clause : whereClauses) {
                SqlInfo clauseInfo = resolvePlaceholdersWithParams(clause, loginAccount);
                if (clauseInfo == null || clauseInfo.getSql() == null || clauseInfo.getSql().trim().isEmpty()) {
                    continue;
                }
                try {
                    Expression expr = CCJSqlParserUtil.parseCondExpression(clauseInfo.getSql());
                    expressions.add(expr);
                    if (clauseInfo.getParameters() != null) {
                        parameters.addAll(clauseInfo.getParameters());
                    }
                } catch (Exception e) {
                    log.error("解析自定义条件表达式失败: {}", clauseInfo.getSql(), e);
                }
            }

            if (expressions.isEmpty()) {
                return null;
            }

            Expression customWhere = expressions.get(0);
            for (int i = 1; i < expressions.size(); i++) {
                customWhere = new AndExpression(customWhere, expressions.get(i));
            }

            if (expressions.size() > 1) {
                customWhere = new Parenthesis(customWhere);
            }

            Expression existingWhere = plainSelect.getWhere();
            if (existingWhere != null) {
                customWhere = new AndExpression(existingWhere, customWhere);
            }

            plainSelect.setWhere(customWhere);
            return new SqlInfo(select.toString(), parameters);
        } catch (Exception e) {
            log.error("注入自定义条件失败: {}", sql, e);
            return null;
        }
    }

    /**
     * 获取表级别的自定义 WHERE 条件
     */
    private List<String> getCustomWhereClauses(String tableName, String dataSourceName) {
        List<String> clauses = new ArrayList<>();
        try {
            if ("datacenter".equalsIgnoreCase(dataSourceName)) {
                QueryWrapper wrapper = QueryWrapper.create()
                        .eq(DatacenterTable::getActualTable, tableName);
                DatacenterTable table = datacenterTableService.getOne(wrapper);
                if (table == null) {
                    List<DatacenterTable> allTables = datacenterTableService.list();
                    for (DatacenterTable t : allTables) {
                        if (t.getActualTable() != null && t.getActualTable().equalsIgnoreCase(tableName)) {
                            table = t;
                            break;
                        }
                    }
                }
                if (table != null) {
                    String clause = extractCustomWhere(table.getOptions());
                    if (clause != null && !clause.isEmpty()) {
                        clauses.add(clause);
                    }
                }
            } else {
                QueryWrapper wrapper = QueryWrapper.create()
                        .eq(DatacenterSourceTable::getName, tableName);
                DatacenterSourceTable table = datacenterSourceTableService.getOne(wrapper);
                if (table == null) {
                    List<DatacenterSourceTable> allTables = datacenterSourceTableService.list();
                    for (DatacenterSourceTable t : allTables) {
                        if (t.getName() != null && t.getName().equalsIgnoreCase(tableName)) {
                            table = t;
                            break;
                        }
                    }
                }
                if (table != null) {
                    String clause = extractCustomWhere(table.getOptions());
                    if (clause != null && !clause.isEmpty()) {
                        clauses.add(clause);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取自定义WHERE条件失败, tableName={}", tableName, e);
        }
        return clauses;
    }

    private String extractCustomWhere(Map<String, Object> options) {
        if (options == null || !options.containsKey("customWhere")) {
            return null;
        }
        Object value = options.get("customWhere");
        return value == null ? null : value.toString().trim();
    }

    private SqlInfo resolvePlaceholdersWithParams(String sql, SysAccount loginAccount) {
        if (sql == null || sql.isEmpty()) {
            return new SqlInfo(sql, new ArrayList<>());
        }
        try {
            // 构建参数上下文
            Map<String, Object> params = buildParamsContext(loginAccount);
            // 使用 SqlTemplate 处理
            SqlTemplate template = SqlTemplate.of(sql);
            return template.format(params);
        } catch (Exception e) {
            log.error("解析动态占位符失败: {}", sql, e);
            return new SqlInfo(sql, new ArrayList<>());
        }
    }

    /**
     * 构建 SqlTemplate 所需的参数上下文
     */
    private Map<String, Object> buildParamsContext(SysAccount loginAccount) {
        Map<String, Object> params = new HashMap<>();

        // account 对象
        params.put("account", loginAccount);

        // dept 对象
        BigInteger deptId = loginAccount.getDeptId();
        if (deptId != null) {
            SysDept dept = sysDeptService.getById(deptId);
            if (dept != null) {
                params.put("dept", dept);
            }
        }

        // role 对象（取第一个角色，或支持多角色的场景使用 roleList）
        BigInteger accountId = loginAccount.getId();
        if (accountId != null) {
            List<SysRole> roles = sysRoleService.getRolesByAccountId(accountId);
            if (roles != null && !roles.isEmpty()) {
                params.put("role", roles.get(0));
                params.put("roleList", roles);
                // 提取角色key列表，用于 IN 查询场景
                List<String> roleKeys = roles.stream()
                        .map(SysRole::getRoleKey)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!roleKeys.isEmpty()) {
                    params.put("roleKeys", roleKeys);
                }
                // 提取角色名称列表
                List<String> roleNames = roles.stream()
                        .map(SysRole::getRoleName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!roleNames.isEmpty()) {
                    params.put("roleNames", roleNames);
                }
                // 提取角色id列表
                List<BigInteger> roleIds = roles.stream()
                        .map(SysRole::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!roleIds.isEmpty()) {
                    params.put("roleIds", roleIds);
                }
            }
        }

        return params;
    }

    /**
     * 根据数据源信息查询 DatacenterSource 实体
     * 只使用四个字段进行精确匹配查询: jdbcUrl + username + sourceName + password
     * 不使用其他查询策略,确保安全性和准确性
     * 
     * @param dataSourceObj DataSource 对象(必须包含 jdbcUrl、username 和 password)
     * @param dataSourceName 数据源名称(sourceName)
     * @return DatacenterSource 实体,如果找不到或无法获取必需字段则返回 null
     */
    private DatacenterSource getDatacenterSource(Object dataSourceObj, String dataSourceName) {
        if (dataSourceName == null || dataSourceName.trim().isEmpty()) {
            log.error("数据源名称为空,无法进行权限检查");
            return null;
        }
        
        try {
            // 从 DataSource 对象中提取必需的信息(JdbcDataSourceInfo 类型)
            String jdbcUrl = null;
            String username = null;
            String password = null;
            
            // 检查 DataSource 对象是否是 JdbcDataSourceInfo 类型
            if (!(dataSourceObj instanceof com.agentsflex.text2sql.entity.JdbcDataSourceInfo)) {
                log.error("无法获取必需的数据源信息(jdbcUrl/username/password),DataSource对象类型不正确: {}, 无法进行权限检查", 
                    dataSourceObj.getClass().getName());
                return null;
            }
            
            com.agentsflex.text2sql.entity.JdbcDataSourceInfo jdbcDataSource = 
                (com.agentsflex.text2sql.entity.JdbcDataSourceInfo) dataSourceObj;
            jdbcUrl = jdbcDataSource.getJdbcUrl();
            username = jdbcDataSource.getUsername();
            password = jdbcDataSource.getPassword();
            
            // 验证必需字段是否存在
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                log.error("无法获取必需的数据源信息(jdbcUrl为空),无法进行权限检查: dataSourceName={}", dataSourceName);
                return null;
            }
            
            if (username == null || username.isEmpty()) {
                log.error("无法获取必需的数据源信息(username为空),无法进行权限检查: dataSourceName={}", dataSourceName);
                return null;
            }
            
            if (password == null || password.isEmpty()) {
                log.error("无法获取必需的数据源信息(password为空),无法进行权限检查: dataSourceName={}", dataSourceName);
                return null;
            }
            
            // 使用四个字段进行精确查询: jdbcUrl + username + sourceName + password
            QueryWrapper wrapper = QueryWrapper.create()
                .eq(DatacenterSource::getSourceAddress, jdbcUrl)
                .eq(DatacenterSource::getUsername, username)
                .eq(DatacenterSource::getSourceName, dataSourceName)
                .eq(DatacenterSource::getPassword, password);
            
            List<DatacenterSource> sources = datacenterSourceService.list(wrapper);
            
            if (sources == null || sources.isEmpty()) {
                log.warn("无法找到匹配的数据源: jdbcUrl={}, username={}, sourceName={}, password={}", 
                    jdbcUrl, username, dataSourceName, password);
                return null;
            }
            
            if (sources.size() == 1) {
                return sources.get(0);
            }
            
            // 如果查询到多个结果,说明数据配置有问题,不应该存在多个相同配置的数据源
            log.warn("查询到多个匹配的数据源(共{}个),这表示数据配置存在问题: jdbcUrl={}, username={}, sourceName={}, password={}", 
                sources.size(), jdbcUrl, username, dataSourceName, password);
            return null;
            
        } catch (Exception e) {
            log.error("查询数据源失败: dataSourceName={}", dataSourceName, e);
            return null;
        }
    }

}
