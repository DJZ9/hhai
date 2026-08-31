package tech.aiflowy.ai.config;

import com.agentsflex.asynctask.AsyncTaskManager;
import com.agentsflex.asynctask.AsyncTaskWorker;
import com.agentsflex.asynctask.handler.InMemoryAsyncTaskHandlerRegistry;
import com.agentsflex.asynctask.policy.ExponentialAsyncTaskRetryPolicy;
import com.agentsflex.asynctask.store.AsyncTaskStore;
import com.agentsflex.asynctask.store.FastjsonAsyncTaskStoreSerializer;
import com.agentsflex.asynctask.store.redis.RedisAsyncTaskStoreConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.aiflowy.ai.asynctask.OcrAsyncTaskHandler;

/**
 * OCR 异步任务队列 Spring 配置：Redis 持久化 Store、Manager、Worker 与重试策略。
 */
@Configuration
@EnableConfigurationProperties(AsyncTaskProperties.class)
@ConditionalOnProperty(prefix = "aiflowy.async-task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncTaskConfig {

    private final AsyncTaskProperties properties;
    private final OcrAsyncTaskHandler ocrHandler;

    public AsyncTaskConfig(AsyncTaskProperties properties, OcrAsyncTaskHandler ocrHandler) {
        this.properties = properties;
        this.ocrHandler = ocrHandler;
    }

    @Bean
    public FastjsonAsyncTaskStoreSerializer asyncTaskStoreSerializer() {
        return new FastjsonAsyncTaskStoreSerializer("tech.aiflowy.");
    }

    @Bean(destroyMethod = "close")
    public RedisAsyncTaskStoreConfig redisAsyncTaskStoreConfig(FastjsonAsyncTaskStoreSerializer serializer) {
        return RedisAsyncTaskStoreConfig.builder(properties.getRedis().getUri())
                .keyPrefix(properties.getRedis().getKeyPrefix())
                .serializer(serializer)
                .build();
    }

    @Bean
    public AsyncTaskStore asyncTaskStore(RedisAsyncTaskStoreConfig config) {
        return config.store();
    }

    @Bean
    public InMemoryAsyncTaskHandlerRegistry asyncTaskHandlerRegistry() {
        return new InMemoryAsyncTaskHandlerRegistry().register(ocrHandler);
    }

    @Bean
    public AsyncTaskManager asyncTaskManager(AsyncTaskStore store, InMemoryAsyncTaskHandlerRegistry registry) {
        return new AsyncTaskManager(store, registry);
    }

    @Bean
    public ExponentialAsyncTaskRetryPolicy asyncTaskRetryPolicy() {
        AsyncTaskProperties.Retry retry = properties.getRetry();
        return new ExponentialAsyncTaskRetryPolicy(
                retry.getQueryIntervalMillis(),
                retry.getInitialErrorDelayMillis(),
                retry.getMaxErrorDelayMillis(),
                retry.getMaxConsecutiveErrors());
    }

    @Bean(destroyMethod = "close")
    public AsyncTaskWorker asyncTaskWorker(AsyncTaskStore store,
                                           InMemoryAsyncTaskHandlerRegistry registry,
                                           ExponentialAsyncTaskRetryPolicy retryPolicy) {
        AsyncTaskProperties.Worker worker = properties.getWorker();
        AsyncTaskWorker asyncTaskWorker = new AsyncTaskWorker(
                "aiflowy-ocr-worker-1", store, registry, retryPolicy, worker.getLeaseMillis());
        asyncTaskWorker.start(worker.getScanIntervalMillis(), worker.getBatchSize());
        return asyncTaskWorker;
    }
}
