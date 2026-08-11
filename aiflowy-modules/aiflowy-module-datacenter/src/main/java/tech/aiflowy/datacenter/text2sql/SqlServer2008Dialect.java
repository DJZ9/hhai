package tech.aiflowy.datacenter.text2sql;

import com.agentsflex.text2sql.entity.ColumnInfo;
import com.agentsflex.text2sql.entity.TableInfo;
import com.agentsflex.text2sql.jdbc.dialect.IDialect;

import java.sql.*;
import java.util.Collection;

public class SqlServer2008Dialect implements IDialect {
    @Override
    public ResultSet getTablesResultSet(DatabaseMetaData dbMeta, Connection conn,
                                        String schema, String[] types) throws SQLException {
        // 直接查询 INFORMATION_SCHEMA.TABLES 避免 DatabaseMetaData 缺陷
        StringBuilder sql = new StringBuilder(
                "SELECT TABLE_NAME, TABLE_SCHEMA AS TABLE_SCHEM, '' AS REMARKS " +
                        "FROM INFORMATION_SCHEMA.TABLES WHERE 1=1"
        );

        if (schema != null && !schema.isEmpty()) {
            sql.append(" AND TABLE_SCHEMA = ?");
        }

        if (types != null && types.length > 0) {
            sql.append(" AND TABLE_TYPE IN (");
            for (int i = 0; i < types.length; i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
            }
            sql.append(")");
        }

        PreparedStatement ps = conn.prepareStatement(sql.toString());
        int paramIndex = 1;
        if (schema != null && !schema.isEmpty()) {
            ps.setString(paramIndex++, schema);
        }
        if (types != null) {
            for (String type : types) {
                String mappedType;
                if ("TABLE".equalsIgnoreCase(type)) {
                    mappedType = "BASE TABLE";
                } else if ("VIEW".equalsIgnoreCase(type)) {
                    mappedType = "VIEW";
                } else {
                    mappedType = type;
                }
                ps.setString(paramIndex++, mappedType);
            }
        }

        return ps.executeQuery();
    }

    @Override
    public void buildTableColumns(String schemaName, TableInfo table,
                                  DatabaseMetaData dbMeta, Connection conn,
                                  Collection<String> ignoreColumns) throws SQLException {
        String tableName = table.getName();
        String schema = (schemaName != null && !schemaName.isEmpty()) ? schemaName : "dbo";

        // 修正：移除 TYPE_NAME，只使用 DATA_TYPE
        String sql =
                "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                        "ORDER BY ORDINAL_POSITION";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                Collection<String> primaryKeys = table.getPrimaryKeys();

                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    if (ignoreColumns != null && ignoreColumns.contains(columnName)) {
                        continue;
                    }

                    ColumnInfo column = new ColumnInfo();
                    column.setName(columnName);
                    // DATA_TYPE 直接返回数据类型名称，如 varchar, int 等
                    column.setType(rs.getString("DATA_TYPE"));

                    String isNullable = rs.getString("IS_NULLABLE");
                    column.setNullable("YES".equalsIgnoreCase(isNullable)? 1 : 0);

                    column.setPrimaryKey(primaryKeys != null && primaryKeys.contains(columnName));

                    table.addColumn(column);
                }
            }
        }
    }
}
