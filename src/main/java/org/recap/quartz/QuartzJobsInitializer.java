package org.recap.quartz;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.batch.job.JobCommonTasklet;
import org.recap.model.jpa.JobEntity;
import org.recap.repository.jpa.JobDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.quartz.CronExpression.isValidExpression;

/**
 * Created by rajeshbabuk on 28/3/17.
 */
@Component
public class QuartzJobsInitializer {

    private static final Logger logger = LoggerFactory.getLogger(QuartzJobsInitializer.class);

    private JobLauncher jobLauncher;
    private JobLocator jobLocator;
    private JobDetailsRepository jobDetailsRepository;
    private Scheduler scheduler;

    /**
     * Instantiates a new Quartz jobs initializer.
     *
     * @param jobLauncher          the job launcher
     * @param jobLocator           the job locator
     * @param jobDetailsRepository the job details repository
     * @param scheduler            the scheduler
     */
    @Autowired
    public QuartzJobsInitializer(JobLauncher jobLauncher, JobLocator jobLocator, JobDetailsRepository jobDetailsRepository, Scheduler scheduler) {
        this.jobLauncher = jobLauncher;
        this.jobLocator = jobLocator;
        this.jobDetailsRepository = jobDetailsRepository;
        this.scheduler = scheduler;
        initializeJobs();
    }

    /**
     * This method reads the jobs from database and initializes them with the quartz scheduler.
     * The jobs without cron expression or unscheduled state are added to scheduler without any trigger.
     * The jobs with cron expression are added to scheduler with a trigger.
     */
    public void initializeJobs() {
        logger.info("Initializing jobs");
        List<JobEntity> jobEntities = jobDetailsRepository.findAll();
        if (CollectionUtils.isNotEmpty(jobEntities)) {
            for (JobEntity jobEntity : jobEntities) {
                String jobName = jobEntity.getJobName();
                String jobStatus = jobEntity.getStatus();
                String cronExpression = jobEntity.getCronExpression();
                try {
                    JobDetailImpl jobDetailImpl = new JobDetailImpl();
                    JobCommonTasklet jobCommonTasklet = new JobCommonTasklet();
                    jobCommonTasklet.setJobDetailImpl(jobDetailImpl, jobName, jobLauncher, jobLocator);
                    if (StringUtils.isNotBlank(cronExpression) && isValidExpression(cronExpression) && !ScsbConstants.UNSCHEDULED.equalsIgnoreCase(jobStatus)) {
                        JobKey jobKey = new JobKey(jobName);
                        jobDetailImpl.setKey(jobKey);
                        CronTriggerImpl trigger = new CronTriggerImpl();
                        trigger.setName(jobName + ScsbConstants.TRIGGER_SUFFIX);
                        trigger.setJobKey(jobKey);
                        trigger.setCronExpression(cronExpression);
                        scheduler.scheduleJob(jobDetailImpl, trigger);
                        logger.info("Job {} is initialized.", jobName);
                    } else {
                        logger.info("Job {} has invalid cron expression and unscheduled state.", jobName);
                        JobKey jobKey = new JobKey(jobName);
                        jobDetailImpl.setKey(jobKey);
                        jobDetailImpl.setDurability(true);
                        scheduler.addJob(jobDetailImpl, true);
                    }
                } catch (Exception ex) {
                    logger.error("Initializing job {} Failed.", jobName);
                    logger.error(ScsbCommonConstants.LOG_ERROR, ex);
                }
            }
        }
    }
}
