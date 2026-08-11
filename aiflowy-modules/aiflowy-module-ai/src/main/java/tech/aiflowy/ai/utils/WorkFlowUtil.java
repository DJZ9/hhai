package tech.aiflowy.ai.utils;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.repository.ChainDefinitionRepository;
import dev.tinyflow.core.chain.repository.ChainStateRepository;
import dev.tinyflow.core.chain.repository.NodeStateRepository;
import dev.tinyflow.core.chain.runtime.ChainExecutor;
import tech.aiflowy.ai.tinyflow.listener.ChainEventListenerForSave;
import tech.aiflowy.common.constant.Constants;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.util.SpringContextUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class WorkFlowUtil {

    public final static String USER_KEY = "user";
    public final static String WORKFLOW_KEY = "workflow";
    public final static String BATCH_ID = "batchId";
    public final static String PRE_ID = "preId";
    public final static String CHAPTER_ID = "chapterId";
    public final static String CHAPTER_TITLE = "chapterTitle";
    public final static String CHAPTER_CONTENT = "chapterContent";

    public static String removeSensitiveInfo(String originJson) {
        JSONObject workflowInfo = JSON.parseObject(originJson);
        JSONArray nodes = workflowInfo.getJSONArray("nodes");
        for (Object node : nodes) {
            JSONObject nodeInfo = (JSONObject) node;
            JSONObject data = nodeInfo.getJSONObject("data");
            JSONObject newData = new JSONObject();
            newData.put("outputDefs", data.get("outputDefs"));
            newData.put("parameters", data.get("parameters"));
            newData.put("title", data.get("title"));
            newData.put("description", data.get("description"));
            nodeInfo.put("data", newData);
        }
        return workflowInfo.toJSONString();
    }

    public static LoginAccount getOperator(Chain chain) {
        Object cache = chain.getState().getMemory().get(Constants.LOGIN_USER_KEY);
        return cache == null ? defaultAccount() : (LoginAccount) cache;
    }

    public static LoginAccount defaultAccount() {
        LoginAccount account = new LoginAccount();
        account.setId(new BigInteger("0"));
        account.setDeptId(new BigInteger("0"));
        account.setTenantId(new BigInteger("0"));
        return account;
    }

    /**
     * 某些场景下不需要全局的 ChainExecutor
     */
    public static ChainExecutor getChainExecutor(boolean needSave) {
        ChainDefinitionRepository chainDefinitionRepository = SpringContextUtil.getBean(ChainDefinitionRepository.class);
        ChainStateRepository chainStateRepository = SpringContextUtil.getBean(ChainStateRepository.class);
        NodeStateRepository nodeStateRepository = SpringContextUtil.getBean(NodeStateRepository.class);
        ChainEventListenerForSave chainEventListenerForSave = SpringContextUtil.getBean(ChainEventListenerForSave.class);
        ChainExecutor chainExecutor = new ChainExecutor(chainDefinitionRepository,
                chainStateRepository,
                nodeStateRepository);
        if (needSave) {
            chainExecutor.addEventListener(chainEventListenerForSave);
        }
        return chainExecutor;
    }

    public static <T> T getContextParam(Chain chain, String key, Class<T> clazz) {
        Object object = chain.getState().getMemory().get(key);
        if (object == null) {
            return null;
        }
        return clazz.cast(object);
    }

    public static JSONArray getNodes(String workflowJson) {
        JSONObject workflowInfo = JSON.parseObject(workflowJson);
        return workflowInfo.getJSONArray("nodes");
    }

    public static JSONObject getNodeByType(String workflowJson, String nodeType) {
        JSONArray nodes = getNodes(workflowJson);
        for (Object node : nodes) {
            JSONObject nodeInfo = (JSONObject) node;
            if (nodeInfo.get("type").equals(nodeType)) {
                return nodeInfo;
            }
        }
        return null;
    }

    public static Map<String, Object> removeEmpty(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (ObjectUtil.isNotEmpty(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
