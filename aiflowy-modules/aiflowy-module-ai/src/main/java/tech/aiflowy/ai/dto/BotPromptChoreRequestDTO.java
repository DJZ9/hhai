package tech.aiflowy.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigInteger;

/**
 * Bot 系统提示词优化请求
 */
public class BotPromptChoreRequestDTO {

    /**
     * 用户输入的提示词
     */
    @NotBlank(message = "prompt不能为空")
    private String prompt;

    /**
     * 聊天机器人的唯一标识符
     */
    @NotNull(message = "botId不能为空")
    private BigInteger botId;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public BigInteger getBotId() {
        return botId;
    }

    public void setBotId(BigInteger botId) {
        this.botId = botId;
    }
}
