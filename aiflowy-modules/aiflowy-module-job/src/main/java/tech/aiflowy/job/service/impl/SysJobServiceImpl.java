package tech.aiflowy.job.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.aiflowy.common.constant.enums.EnumMisfirePolicy;
import tech.aiflowy.job.entity.SysJob;
import tech.aiflowy.job.job.JobConstant;
import tech.aiflowy.job.job.QuartzJob;
import tech.aiflowy.job.job.QuartzJobNoConcurrent;
import tech.aiflowy.job.mapper.SysJobMapper;
import tech.aiflowy.job.service.SysJobService;
import tech.aiflowy.job.util.JobUtil;

import javax.annotation.Resource;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collection;

/**
 * 系统任务表 服务层实现。
 *
 * @author xiaoma
 * @since 2025-05-20
 */
@Service
public class SysJobServiceImpl extends ServiceImpl<SysJobMapper, SysJob>  implements SysJobService {

    protected Logger log = LoggerFactory.getLogger(SysJobServiceImpl.class);

    @Resource
    private Scheduler scheduler;

    @Override
    public void test() {
        System.out.println("java bean 动态执行");
    }

    @Override
    public void testParam(String a, Boolean b, Integer c) {
        System.out.println("动态执行spring bean，执行参数：" + "a="+ a + ",b="+ b + ",c="+ c);
    }

    @Override
    public void addJob(SysJob job) {
        Integer allowConcurrent = job.getAllowConcurrent();
        Class<? extends Job> jobClass = allowConcurrent  == 1 ? QuartzJob.class : QuartzJobNoConcurrent.class;

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(JobUtil.getJobKey(job))
                .build();

        jobDetail.getJobDataMap().put(JobConstant.JOB_MAP_BEAN_NAME, job);

        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule(job.getCronExpression());

        Integer misfirePolicy = job.getMisfirePolicy();
        if (EnumMisfirePolicy.MISFIRE_DO_NOTHING.getCode() == misfirePolicy) {
            cron.withMisfireHandlingInstructionDoNothing();
        }
        if (EnumMisfirePolicy.MISFIRE_FIRE_AND_PROCEED.getCode() == misfirePolicy) {
            cron.withMisfireHandlingInstructionFireAndProceed();
        }
        if (EnumMisfirePolicy.MISFIRE_IGNORE_MISFIRES.getCode() == misfirePolicy) {
            cron.withMisfireHandlingInstructionIgnoreMisfires();
        }

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(JobUtil.getTriggerKey(job))
                .withSchedule(cron).build();

        try {
            scheduler.scheduleJob(jobDetail,trigger);
        } catch (SchedulerException e) {
            log.error("启动任务失败：", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteJob(Collection<Serializable> ids) {
        try {
            for (Serializable id : ids) {
                SysJob sysJob = new SysJob();
                sysJob.setId(new BigInteger(id.toString()));
                scheduler.deleteJob(JobUtil.getJobKey(sysJob));
            }
        } catch (SchedulerException e) {
            log.error("删除任务失败：", e);
            throw new RuntimeException(e);
        }
    }
}
