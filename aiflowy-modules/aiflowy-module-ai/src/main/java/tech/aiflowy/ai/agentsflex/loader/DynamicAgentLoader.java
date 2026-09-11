package tech.aiflowy.ai.agentsflex.loader;

import com.agentsflex.agent.Agent;
import com.agentsflex.agent.loader.AgentLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 动态 Agent 注册器，支持运行时注册和加载 Agent 实例。
 * <p>
 * 替代不可变的 {@link com.agentsflex.agent.loader.InMemoryAgentLoader}，
 * 供 AgentRunner 恢复挂起的 Turn 时按 agentId 加载 Agent（含 ChatModel/Tool/Middleware 等运行时对象）。
 * </p>
 * <p>
 * Agent 在 {@code BotServiceImpl.startChat()} 中创建后通过 {@link #register(Agent)} 注册，
 * 恢复时通过 {@link #load(String, String)} 或 {@link #loadActive(String)} 加载。
 * Agent 存储在内存中（ConcurrentHashMap），服务重启后丢失。
 * </p>
 */
@Component
public class DynamicAgentLoader implements AgentLoader {

    /** 按 Agent ID 索引的 Agent 实例，同一 ID 后注册的覆盖先前的 */
    private final ConcurrentMap<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * 运行时注册 Agent 实例
     *
     * @param agent 要注册的 Agent，不能为 null
     */
    public void register(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("agent must not be null");
        }
        agents.put(agent.getId(), agent);
    }

    /**
     * 移除已注册的 Agent（可选操作，用于清理过期 Agent）
     *
     * @param agentId Agent ID
     */
    public void unregister(String agentId) {
        if (agentId != null) {
            agents.remove(agentId);
        }
    }

    /**
     * 按稳定 ID 和版本加载 Agent。
     * <p>
     * 当前实现忽略 version 参数，返回最新注册的 Agent。
     * 这是因为同一会话内 Agent 配置不会变更，恢复时使用最新注册的即可。
     * </p>
     *
     * @param agentId Agent 的稳定标识
     * @param version Agent 配置版本（当前忽略，返回最新注册的）
     * @return Agent 实例，不存在时返回 null
     */
    @Override
    public Agent load(String agentId, String version) {
        if (agentId == null) {
            return null;
        }
        return agents.get(agentId);
    }

    /**
     * 按稳定 ID 加载当前生效的 Agent
     *
     * @param agentId Agent 的稳定标识
     * @return Agent 实例，不存在时返回 null
     */
    @Override
    public Agent loadActive(String agentId) {
        if (agentId == null) {
            return null;
        }
        return agents.get(agentId);
    }
}
