package tech.aiflowy.common.constant.enums;

import tech.aiflowy.common.annotation.DictDef;

@DictDef(name = "批量执行状态", code = "batchExecStatus", keyField = "code", labelField = "text")
public enum EnumBatchExec {

    WAITING(0, "待执行"),
    EXECUTING(1, "执行中"),
    EXECUTED(2, "执行完成"),
    FAILED(3, "执行失败"),
    ;

    private Integer code;
    private String text;

    EnumBatchExec(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    public static EnumBatchExec getByCode(Integer code) {
        if (null == code) {
            return null;
        }
        for (EnumBatchExec type : EnumBatchExec.values()) {
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
