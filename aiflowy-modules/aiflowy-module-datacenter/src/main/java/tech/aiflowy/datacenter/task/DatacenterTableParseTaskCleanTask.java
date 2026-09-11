package tech.aiflowy.datacenter.task;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.tenant.TenantManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.aiflowy.datacenter.entity.DatacenterTableParseTask;
import tech.aiflowy.datacenter.service.DatacenterTableParseTaskService;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 定时清理解析任务数据
 * 每天早上2点清理前一天的所有解析任务数据
 */
@Component
public class DatacenterTableParseTaskCleanTask {

    private static final Logger log = LoggerFactory.getLogger(DatacenterTableParseTaskCleanTask.class);

    @Resource
    private DatacenterTableParseTaskService parseTaskService;

    /**
     * 定时任务，每天早上2点清理前一天的解析任务数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanParseTask() {
        log.info("开始清理前一天的解析任务数据");
        try {
            // 忽略租户条件
            TenantManager.ignoreTenantCondition();

            // 计算前一天的时间范围
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Date startOfDay = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endOfDay = Date.from(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            // 查询前一天创建的所有任务
            QueryWrapper wrapper = QueryWrapper.create();
            wrapper.ge(DatacenterTableParseTask::getCreated, startOfDay);
            wrapper.lt(DatacenterTableParseTask::getCreated, endOfDay);

            List<DatacenterTableParseTask> tasks = parseTaskService.list(wrapper);

            if (tasks != null && !tasks.isEmpty()) {
                // 批量删除
                List<Long> ids = tasks.stream()
                        .map(task -> task.getId().longValue())
                        .toList();
                parseTaskService.removeByIds(ids);
                log.info("清理完成，共删除 {} 条数据中枢解析任务数据", tasks.size());
            }
        } catch (Exception e) {
            log.error("清理解析任务数据失败", e);
        } finally {
            // 恢复租户条件
            TenantManager.restoreTenantCondition();
        }
    }
}
