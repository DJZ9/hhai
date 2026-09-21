package tech.aiflowy.common.dict.loader;

import tech.aiflowy.common.tree.Tree;
import tech.aiflowy.common.util.RequestUtil;
import tech.aiflowy.common.util.SqlIdentifierUtil;
import tech.aiflowy.common.dict.Dict;
import tech.aiflowy.common.dict.DictItem;
import tech.aiflowy.common.dict.DictLoader;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 数据表字典加载器：把某张表的 key 列、label 列当作字典项。
 * <p>
 * 表名、列名无法使用 JDBC "?" 占位符，只能拼进 SQL 文本，因此这些取值全部由
 * {@link SqlIdentifierUtil} 校验为“纯标识符”，并以反引号引用后再拼 SQL。
 * <p>
 * 构造函数不做校验（避免历史数据在启动装配阶段直接让应用启动失败），校验放在
 * {@link #buildSelectSql(String, String, String, String, String, String, boolean)} 里，
 * 即每次执行 SQL 之前。
 */
public class DatabaseDictLoader implements DictLoader {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseDictLoader.class);

    private static final Object[] EMPTY_PARAMS = new Object[0];
    private String code;
    private String tableName;
    private String keyColumn;
    private String labelColumn;
    private String parentColumn;
    private String orderBy;

    public DatabaseDictLoader(String code, String tableName, String keyColumn, String labelColumn) {
        this.code = code;
        this.tableName = tableName;
        this.keyColumn = keyColumn;
        this.labelColumn = labelColumn;
    }

    public DatabaseDictLoader(String code, String tableName, String keyColumn, String labelColumn, String parentColumn) {
        this.code = code;
        this.tableName = tableName;
        this.keyColumn = keyColumn;
        this.labelColumn = labelColumn;
        this.parentColumn = parentColumn;
    }

    public DatabaseDictLoader(String code, String tableName, String keyColumn, String labelColumn, String parentColumn, String orderBy) {
        this.code = code;
        this.tableName = tableName;
        this.keyColumn = keyColumn;
        this.labelColumn = labelColumn;
        this.parentColumn = parentColumn;
        this.orderBy = orderBy;
    }

    @Override
    public String code() {
        return code;
    }


    @Override
    public Dict load(String keyword, Map<String, String[]> parameters) {
        boolean withKeyword = StringUtils.hasText(keyword);
        String sql = buildSelectSql(code, tableName, keyColumn, labelColumn, parentColumn, orderBy, withKeyword);

        List<Row> rows = Db.selectListBySql(sql, withKeyword ? new Object[]{keyword.trim()} : EMPTY_PARAMS);
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        List<DictItem> items = new ArrayList<>(rows.size());

        Boolean asTree = RequestUtil.getParamAsBoolean(parameters,"asTree");

        //有树形结构
        if (StringUtils.hasText(parentColumn)) {
            List<Row> topLayerRows = findTopLayerRows(rows);
            //以树形结构输出
            if (asTree != null && asTree) {
                makeTree(topLayerRows, items, rows);
            }
            //以平级结构输出
            else {
                makeLayer(0, topLayerRows, items, rows);
            }
        }
        //无树形结构数据
        else {
            for (Row row : rows) {
                DictItem dictItem = new DictItem();
                dictItem.setValue(row.get(keyColumn));
                dictItem.setLabel(String.valueOf(row.get(labelColumn)));
                items.add(dictItem);
            }
        }

        Dict dict = new Dict();
        dict.setCode(code);
        dict.setItems(items);
        return dict;
    }

    /**
     * 校验字典配置并拼出查询 SQL。取值非法时直接抛异常，绝不执行 SQL。
     * <p>
     * 表名、列名、ORDER BY 列名都会被校验为合法标识符（字母、数字、下划线或 $，最多 3 段点分），
     * 再以反引号引用拼入 SQL；keyword 仍作为 JDBC "?" 参数绑定，不参与字符串拼接。
     *
     * @param code         字典编码，仅用于日志与异常提示
     * @param tableName    表名，必填
     * @param keyColumn    key 列名，必填
     * @param labelColumn  label 列名，必填
     * @param parentColumn 父级列名，选填
     * @param orderBy      排序表达式，选填，仅允许 {@code 列名 [asc|desc]} 逗号列表
     * @param withKeyword  是否附带 keyword 等值条件
     * @return 渲染后的 SELECT 语句
     */
    static String buildSelectSql(String code, String tableName, String keyColumn, String labelColumn,
                                 String parentColumn, String orderBy, boolean withKeyword) {
        String sqlKeyColumn = checkAndQuote(code, "keyColumn", keyColumn);
        String sqlLabelColumn = checkAndQuote(code, "labelColumn", labelColumn);
        String sqlTableName = checkAndQuote(code, "tableName", tableName);

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(sqlKeyColumn)
                .append(", ")
                .append(sqlLabelColumn);

        if (StringUtils.hasText(parentColumn)) {
            sql.append(", ").append(checkAndQuote(code, "parentColumn", parentColumn));
        }
        sql.append(" FROM ").append(sqlTableName);
        if (withKeyword) {
            sql.append(" WHERE ").append(sqlLabelColumn).append(" = ?");
        }
        if (StringUtils.hasText(orderBy)) {
            sql.append(" ORDER BY ").append(checkAndQuoteOrderBy(code, orderBy));
        }
        return sql.toString();
    }

    private static String checkAndQuote(String code, String field, String value) {
        try {
            return SqlIdentifierUtil.quoteIdentifier(value);
        } catch (IllegalArgumentException e) {
            throw rejectConfig(code, field, e);
        }
    }

    private static String checkAndQuoteOrderBy(String code, String value) {
        try {
            return SqlIdentifierUtil.quoteOrderBy(value);
        } catch (IllegalArgumentException e) {
            throw rejectConfig(code, "orderBy", e);
        }
    }

    /**
     * 记录告警日志后抛出异常。
     */
    private static IllegalArgumentException rejectConfig(String code, String field, IllegalArgumentException cause) {
        String message = "数据表字典 [" + code + "] 的 " + field + " 配置非法，已拒绝执行 SQL：" + cause.getMessage();
        logger.warn(message);
        return new IllegalArgumentException(message);
    }

    private void makeTree(List<Row> parentRows, List<DictItem> parentItems, List<Row> allRows) {
        for (Row parentRow : parentRows) {
            DictItem parentItem = row2DictItem(0, parentRow);
            parentItems.add(parentItem);

            List<Row> children = new ArrayList<>();
            for (Row maybeChild : allRows) {
                if (Objects.equals(maybeChild.get(parentColumn), parentRow.get(keyColumn))) {
                    children.add(maybeChild);
                }
            }
            if (!children.isEmpty()) {
                List<DictItem> childrenItems = new ArrayList<>(children.size());
                parentItem.setChildren(childrenItems);
                makeTree(children, childrenItems, allRows);
            }
        }
    }

    private void makeLayer(int layerNo, List<Row> parentRows, List<DictItem> parentItems, List<Row> allRows) {
        for (Row parentRow : parentRows) {
            parentItems.add(row2DictItem(layerNo, parentRow));

            List<Row> children = new ArrayList<>();
            for (Row maybeChild : allRows) {
                if (Objects.equals(maybeChild.get(parentColumn), parentRow.get(keyColumn))) {
                    children.add(maybeChild);
                }
            }
            if (!children.isEmpty()) {
                makeLayer(layerNo + 1, children, parentItems, allRows);
            }
        }
    }


    private DictItem row2DictItem(int layerNo, Row row) {
        DictItem dictItem = new DictItem();
        dictItem.setValue(row.get(keyColumn));
        dictItem.setLabel(Tree.getPrefix(layerNo) + row.get(labelColumn));
        dictItem.setLayerNo(layerNo);
        return dictItem;
    }


    private List<Row> findTopLayerRows(List<Row> rows) {
        List<Row> topLayerRows = new ArrayList<>();
        for (Row row : rows) {
            boolean foundParent = false;
            for (Row row1 : rows) {
                if (Objects.equals(row1.get(keyColumn), row.get(parentColumn))) {
                    foundParent = true;
                    break;
                }
            }
            if (!foundParent) {
                topLayerRows.add(row);
            }
        }
        return topLayerRows;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getKeyColumn() {
        return keyColumn;
    }

    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    public String getLabelColumn() {
        return labelColumn;
    }

    public void setLabelColumn(String labelColumn) {
        this.labelColumn = labelColumn;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }
}
