package tech.aiflowy.datacenter.utils;

import java.util.List;

public class SqlInfo {

    private final String sql;
    private final List<Object> parameters;

    public SqlInfo(String sql, List<Object> parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "SqlFormatResult{" + "sql='" + sql + '\'' + ", parameters=" + parameters + '}';
    }
}