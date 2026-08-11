package tech.aiflowy.datacenter.utils;

import com.agentsflex.text2sql.jdbc.dialect.IDialect;
import tech.aiflowy.common.constant.enums.EnumDatasourceType;
import tech.aiflowy.datacenter.text2sql.SqlServer2008Dialect;

public class Text2SqlUtils {

    public static IDialect getDialect(Integer dbType) {
        IDialect dialect = IDialect.DEFAULT;
        if (EnumDatasourceType.MYSQL.getCode().equals(dbType)) {
            dialect = IDialect.MYSQL;
        }
        if (EnumDatasourceType.ORACLE.getCode().equals(dbType)) {
            dialect = IDialect.ORACLE;
        }
        if (EnumDatasourceType.POSTGRESQL.getCode().equals(dbType)) {
            dialect = IDialect.POSTGRESQL;
        }
        if (EnumDatasourceType.SQL_SERVER.getCode().equals(dbType)) {
            dialect = new SqlServer2008Dialect();
        }
        return dialect;
    }
}
