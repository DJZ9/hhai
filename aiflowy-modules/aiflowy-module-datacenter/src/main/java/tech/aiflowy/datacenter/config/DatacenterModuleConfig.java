package tech.aiflowy.datacenter.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@MapperScan("tech.aiflowy.datacenter.mapper")
@EnableAsync
@EnableScheduling
public class DatacenterModuleConfig {

    public DatacenterModuleConfig() {
        System.out.println("启用模块 >>>>>>>>>> module-datacenter");
    }

    /**
     * 数据中枢异步导入线程池
     */
    @Bean("datacenterImportThreadPool")
    public ThreadPoolTaskExecutor datacenterImportThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("datacenter-import-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
