package tech.aiflowy.ai.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.agentsflex.core.model.chat.BaseChatConfig;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.client.StreamContext;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.ChainStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import tech.aiflowy.ai.entity.ModelTrigger;
import tech.aiflowy.ai.entity.WorkflowExecResult;
import tech.aiflowy.ai.tinyflow.event.PreEventEntity;
import tech.aiflowy.common.constant.enums.EnumBatchExec;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.util.Maps;
import tech.aiflowy.common.util.SpringContextUtil;

import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

public class EventUtil {

    private static final Logger log = LoggerFactory.getLogger(EventUtil.class);

    /**
     * 流式聊天事件，对话结束时调用
     */
    public static void publishStreamChatEvent(StreamContext context, long chatTime, long streamTime) {

        try {
            ApplicationEventPublisher publisher = SpringContextUtil.getApplicationContext();

            BaseChatConfig config = context.getChatContext().getConfig();
            Map<String, Object> customProperties = config.getCustomProperties();
            BigInteger modelId = (BigInteger) customProperties.get("modelId");
            BigInteger botId = (BigInteger) customProperties.get("botId");
            LoginAccount account = (LoginAccount) customProperties.get("account");

            ModelTrigger event = new ModelTrigger();

            event.setTenantId(account.getTenantId());
            event.setDeptId(account.getDeptId());
            event.setModelId(modelId);
            event.setProvider(config.getProvider());
            event.setModelName(config.getModel());
            event.setTotalTokens(context.getFullMessage().getTotalTokens());
            event.setBotId(botId);
            event.setAccountId(account.getId());
            event.setCreated(new Date());
            event.setCreatedDay(new Date());
            event.setCreatedBy(account.getId());
            event.setChatTime(chatTime);
            event.setStreamTime(streamTime);

            publisher.publishEvent(event);
        } catch (Exception e) {
            log.error("publishStreamChatEvent error", e);
        }

    }

    /**
     * 普通聊天事件
     */
    public static void publishChatEvent(BaseChatConfig config, AiMessageResponse response, long chatTime, long nonStreamTime) {

        try {
            ApplicationEventPublisher publisher = SpringContextUtil.getApplicationContext();

            ModelTrigger event = new ModelTrigger();

            String provider = config.getProvider();
            Map<String, Object> customProperties = config.getCustomProperties();
            Object modelId = customProperties.get("modelId");
            if (modelId == null) {
                return;
            }
            Chain currentChain = Chain.currentChain();

            // 工作流触发
            LoginAccount account;
            if (currentChain != null) {
                account = WorkFlowUtil.getOperator(currentChain);
                event.setWorkflowId(new BigInteger(currentChain.getDefinition().getId()));
            } else {
                // bot触发
                Object botId = customProperties.get("botId");
                if (botId == null) {
                    return;
                }
                account = (LoginAccount) customProperties.get("account");
                event.setBotId((BigInteger) botId);
            }
            Integer totalTokens = response.getMessage().getTotalTokens();

            event.setTenantId(account.getTenantId());
            event.setDeptId(account.getDeptId());
            event.setModelId((BigInteger) modelId);
            event.setProvider(provider);
            event.setModelName(config.getModel());
            event.setTotalTokens(totalTokens);
            event.setAccountId(account.getId());
            event.setCreated(new Date());
            event.setCreatedDay(new Date());
            event.setCreatedBy(account.getId());
            event.setChatTime(chatTime);
            event.setNonStreamTime(nonStreamTime);

            publisher.publishEvent(event);

        } catch (Exception e) {
            log.error("publishChatEvent error", e);
        }

    }

    public static void publishPreEvent(Chain chain, WorkflowExecResult record) {
        try {
            ApplicationEventPublisher publisher = SpringContextUtil.getApplicationContext();
            BigInteger preId = WorkFlowUtil.getContextParam(chain, WorkFlowUtil.PRE_ID, BigInteger.class);
            PreEventEntity event = new PreEventEntity();
            event.setPreId(preId);
            int status = EnumBatchExec.FAILED.getCode();
            if (ChainStatus.SUCCEEDED.getValue() == record.getStatus()) {
                status = EnumBatchExec.EXECUTED.getCode();
            }
            event.setStatus(status);
            event.setOutput(record.getOutput());
            event.setStartTime(record.getStartTime());
            event.setEndTime(record.getEndTime());
            publisher.publishEvent(event);
        } catch (Exception e) {
            log.error("publishPreEvent error", e);
        }
    }

    public static void setAccount(Maps prop) {
        if (StpUtil.isLogin()) {
            prop.set("account", SaTokenUtil.getLoginAccount());
        } else {
            prop.set("account", WorkFlowUtil.defaultAccount());
        }
    }
}
