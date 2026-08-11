package tech.aiflowy.common.constant.enums;

import tech.aiflowy.common.annotation.DictDef;

@DictDef(name = "权限设置", code = "deptAuthType", keyField = "code", labelField = "text")
public enum EnumDeptMappingAuthType {

    ALL(0,"所有人"),
    CUSTOM(1,"自定义");

    private final int code;
    private final String text;

    EnumDeptMappingAuthType(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }
}
