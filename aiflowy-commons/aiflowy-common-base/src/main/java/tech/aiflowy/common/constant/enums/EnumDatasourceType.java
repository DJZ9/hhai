package tech.aiflowy.common.constant.enums;

import tech.aiflowy.common.annotation.DictDef;

@DictDef(name = "数据源类型", code = "datasourceType", keyField = "code", labelField = "text")
public enum EnumDatasourceType {

    MYSQL(0, "MySQL"),
    ORACLE(1, "Oracle"),
    SQL_SERVER(2, "SQL Server"),
    POSTGRESQL(3, "PostgreSQL"),
    OCEAN_BASE(4, "OceanBase"),
    DM(5, "达梦"),
    GAUSS(6, " GaussDB"),
    ;

    private Integer code;
    private String text;

    EnumDatasourceType(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    public static EnumDatasourceType getByCode(Integer code) {
        if (null == code) {
            return null;
        }
        for (EnumDatasourceType type : EnumDatasourceType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new RuntimeException("内容类型非法");
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }}
