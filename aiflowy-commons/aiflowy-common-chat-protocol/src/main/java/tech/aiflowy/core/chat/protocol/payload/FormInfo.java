package tech.aiflowy.core.chat.protocol.payload;

import java.io.Serializable;
import java.math.BigInteger;

public class FormInfo implements Serializable {

    public static final long serialVersionUID = 1L;

    private String formId;
    private String toolCallId;
    private BigInteger botId;
    private BigInteger conversationId;
    private BigInteger messageId;
    private String chainStateId;

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public BigInteger getBotId() {
        return botId;
    }

    public void setBotId(BigInteger botId) {
        this.botId = botId;
    }

    public BigInteger getConversationId() {
        return conversationId;
    }

    public void setConversationId(BigInteger conversationId) {
        this.conversationId = conversationId;
    }

    public BigInteger getMessageId() {
        return messageId;
    }

    public void setMessageId(BigInteger messageId) {
        this.messageId = messageId;
    }

    public String getChainStateId() {
        return chainStateId;
    }

    public void setChainStateId(String chainStateId) {
        this.chainStateId = chainStateId;
    }
}
