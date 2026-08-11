package tech.aiflowy.core.chat.protocol.payload;

import java.util.List;

public class FormData {

    /**
     * 表单属性名称
     */
    private String attrName;
    /**
     * 类型 input、select、radio、checkbox 等
     */
    private String type;
    /**
     * 表单标签
     */
    private String label;

    /**
     * 内容类型
     */
    private String contentType;
    /**
     * 选项
     */
    private List<Object> options;
    /**
     * 是否必填
     */
    private boolean required;

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public List<Object> getOptions() {
        return options;
    }

    public void setOptions(List<Object> options) {
        this.options = options;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
