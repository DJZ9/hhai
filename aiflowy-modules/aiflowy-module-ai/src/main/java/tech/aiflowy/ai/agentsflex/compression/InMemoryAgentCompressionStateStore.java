package tech.aiflowy.ai.agentsflex.compression;

import com.agentsflex.agent.compression.AgentContextCompressionState;
import com.agentsflex.agent.compression.AgentContextCompressionStateStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版 Agent 压缩状态存储。
 * <p>
 * 使用 {@link ConcurrentHashMap} 按会话保存压缩状态，并对 {@code save} 实现乐观锁语义
 * （传入的期望版本号与当前状态版本号不一致时写入失败）。
 * </p>
 * <p>
 * 注意：状态保存在进程内存中，服务重启后丢失，多实例部署时实例之间不共享。
 * </p>
 */
public class InMemoryAgentCompressionStateStore implements AgentContextCompressionStateStore {

    private final ConcurrentMap<String, AgentContextCompressionState> states = new ConcurrentHashMap<>();

    @Override
    public AgentContextCompressionState load(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        return states.get(conversationId);
    }

    @Override
    public boolean save(String conversationId, AgentContextCompressionState state, long expectedVersion) {
        if (conversationId == null || state == null) {
            return false;
        }
        boolean[] saved = new boolean[1];
        states.compute(conversationId, (key, existing) -> {
            long currentVersion = existing == null ? 0L : existing.getVersion();
            if (currentVersion != expectedVersion) {
                // 版本冲突：保留原状态，写入失败
                return existing;
            }
            saved[0] = true;
            return state;
        });
        return saved[0];
    }
}
