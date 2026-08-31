package tech.aiflowy.ai.agentsflex.memory;

import com.agentsflex.core.memory.ChatMemory;
import com.agentsflex.core.message.Message;
import com.agentsflex.core.util.CollectionUtil;
import com.mybatisflex.core.query.QueryWrapper;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.ai.service.BotMessageService;

import java.math.BigInteger;
import java.util.*;

public class BotMessageMemory implements ChatMemory {
    private final BigInteger botId;
    private final BigInteger accountId;
    private final BigInteger conversationId;
    private final BotMessageService messageService;

    private List<Message> messages = new ArrayList<>();

    public BotMessageMemory(BigInteger botId, BigInteger accountId, BigInteger conversationId,
                            BotMessageService messageService) {
        this.botId = botId;
        this.accountId = accountId;
        this.conversationId = conversationId;
        this.messageService = messageService;
    }

    @Override
    public List<Message> getMessages(int count) {
        if (messages.size() > 1) {
            return messages;
        }
        List<BotMessage> recentMessages = messageService.list(QueryWrapper.create()
                .eq(BotMessage::getBotId, botId, true)
                .eq(BotMessage::getAccountId, accountId, true)
                .eq(BotMessage::getConversationId, conversationId, true)
                .orderBy(BotMessage::getCreated, false)
                .limit(count)
        );
        if (CollectionUtil.noItems(recentMessages)) {
            return Collections.emptyList();
        }

        Collections.reverse(recentMessages);

        List<Message> messages = new ArrayList<>(recentMessages.size());
        for (BotMessage botMessage : recentMessages) {
            Message message = botMessage.getContentAsMessage();
            if (message != null) messages.add(message);
        }
        return messages;
    }

    @Override
    public List<Message> getMessages(int offset, int count) {
        return List.of();
    }

    @Override
    public List<Message> getModelMessages(int count) {
        List<Message> allMessages = getMessages(count);
        if (allMessages == null || allMessages.isEmpty()) {
            return Collections.emptyList();
        }
        return allMessages;
    }


    @Override
    public void addMessage(Message message) {
        this.messages.add(message);
        BotMessage dbMessage = new BotMessage();
        dbMessage.setCreated(new Date());
        dbMessage.setBotId(botId);
        dbMessage.setAccountId(accountId);
        dbMessage.setConversationId(conversationId);
        dbMessage.setContentAndRole(message);
        dbMessage.setModified(new Date());
        messageService.save(dbMessage);
    }

    @Override
    public Message getMessage(String messageId) {
        return null;
    }

    @Override
    public boolean addMessageIfAbsent(Message message) {
        return false;
    }

    @Override
    public boolean updateMessage(Message message, long expectedVersion) {
        return false;
    }

    @Override
    public void addMessages(Collection<? extends Message> messages) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Object id() {
        return botId;
    }

}
