package tech.aiflowy.ai.agentsflex.message;

import com.agentsflex.core.message.AbstractTextMessage;

import java.math.BigInteger;
import java.util.HashMap;

public class FormMessage extends AbstractTextMessage<FormMessage> {

    /**
     * tb_bot_message 的 id，更新表单内容时使用
     */
    private BigInteger messageId;

    @Override
    public FormMessage copy() {
        FormMessage copy = new FormMessage();
        copy.content = this.content;
        if (this.metadataMap != null) {
            copy.metadataMap = new HashMap(this.metadataMap);
        }
        return copy;
    }

    public BigInteger getMessageId() {
        return messageId;
    }

    public void setMessageId(BigInteger messageId) {
        this.messageId = messageId;
    }
}
