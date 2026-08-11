package tech.aiflowy.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    /**
     * 通用异步任务线程池（如：对话压缩等后台任务）
     */
    @Bean("commonExecutor")
    public ThreadPoolTaskExecutor commonExecutor() {
        int cpuCoreNum = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 通用IO密集型（DB/HTTP/Redis）4核最优
        int corePoolSize = cpuCoreNum * 4;   // 4核=16
        int maxPoolSize = Math.max(corePoolSize * 2, 32); // 确保 maxPoolSize > corePoolSize，最小32
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(500);             // 队列 500
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("CommonAsync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    /**
     * SSE消息发送专用线程池
     * 核心原则：IO密集型任务（网络推送），线程数 = CPU核心数 * 2 + 1
     */
    @Bean(name = "sseThreadPool")
    public ThreadPoolTaskExecutor sseThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int cpuCoreNum = Runtime.getRuntime().availableProcessors(); // 获取CPU核心数（4核返回4）
        executor.setCorePoolSize(cpuCoreNum * 2); // 核心线程数
        executor.setMaxPoolSize(cpuCoreNum * 10); // 最大线程数（峰值时扩容，避免线程过多导致上下文切换）
        executor.setQueueCapacity(8000); // 任务队列容量
        executor.setKeepAliveSeconds(30); // 空闲线程存活时间：30秒（非核心线程空闲后销毁，节省资源）
        executor.setThreadNamePrefix("sse-sender-");

        // 拒绝策略
        executor.setRejectedExecutionHandler((runnable, executorService) -> {
            log.error("SSE线程池过载！核心线程数：{}，最大线程数：{}，队列任务数：{}",
                    executorService.getCorePoolSize(),
                    executorService.getMaximumPoolSize(),
                    executorService.getQueue().size());
            // 抛出自定义异常，全局捕获后返回“服务繁忙”
            throw new BusinessException("服务器忙，请稍后重试");
        });
        executor.initialize();
        return executor;
    }
}
