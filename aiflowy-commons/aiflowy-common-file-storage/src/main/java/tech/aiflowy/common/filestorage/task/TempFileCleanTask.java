package tech.aiflowy.common.filestorage.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.filestorage.xFileStorage.StorageConfig;
import tech.aiflowy.common.util.SpringContextUtil;

import java.time.LocalDate;

/**
 * 临时文件定时清理任务
 * 每天凌晨2点清理前一天上传的临时文件
 * 根据存储类型自动选择清理方式（本地存储或云存储）
 */
@Component
public class TempFileCleanTask {

    private static final Logger log = LoggerFactory.getLogger(TempFileCleanTask.class);

    /**
     * 定时清理临时文件，每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanTempFiles() {
        log.info("开始清理前一天的临时文件");

        try {
            // 获取存储配置
            StorageConfig storageConfig = StorageConfig.getInstance();
            if (storageConfig == null) {
                log.warn("未找到存储配置，跳过临时文件清理");
                return;
            }

            String storageType = storageConfig.getType();
            log.info("当前存储类型: {}", storageType);

            // 根据存储类型获取对应的FileStorageService实现
            String beanName = getBeanName(storageType);
            FileStorageService fileStorageService = SpringContextUtil.getBean(beanName, FileStorageService.class);
            if (fileStorageService == null) {
                log.warn("未找到存储服务实现，存储类型: {}", storageType);
                return;
            }

            // 获取前一天的日期
            LocalDate yesterday = LocalDate.now().minusDays(1);

            // 调用清理方法
            int deletedCount = fileStorageService.cleanTempFiles(yesterday);

            log.info("临时文件清理完成，存储类型: {}，删除 {} 个文件", storageType, deletedCount);

        } catch (Exception e) {
            log.error("清理临时文件失败", e);
        }
    }

    /**
     * 获取存储类型对应的Bean名称
     */
    private String getBeanName(String storageType) {
        if ("local".equals(storageType)) {
            return "local";
        } else if ("xFileStorage".equals(storageType)) {
            return "xFileStorage";
        }
        return storageType;
    }
}