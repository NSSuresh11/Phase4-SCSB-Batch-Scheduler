package org.recap.batch.service;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronExpression;
import org.recap.ScsbConstants;
import org.recap.model.jpa.JobEntity;
import org.recap.repository.jpa.JobDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created by rajeshbabuk on 12/4/17.
 */
@Service
public class UpdateJobDetailsService {

    @Autowired
    private JobDetailsRepository jobDetailsRepository;

    @Autowired
    protected CommonService commonService;

    /**
     * Gets job details repository.
     *
     * @return the job details repository
     */
    public JobDetailsRepository getJobDetailsRepository() {
        return jobDetailsRepository;
    }

    /**
     * This method makes a rest call to solr client microservice to update the job with next execution time.
     *
     * @param solrClientUrl    the solr client url
     * @param jobName          the job name
     * @param lastExecutedTime the last executed time
     * @return status of updating the job
     */
    public String updateJob(String solrClientUrl, String jobName, Date lastExecutedTime, Long jobInstanceId) throws Exception {
        JobEntity jobEntity = getJobDetailsRepository().findByJobName(jobName);
        jobEntity.setLastExecutedTime(lastExecutedTime);
        jobEntity.setJobInstanceId(jobInstanceId.intValue());
        if (StringUtils.isNotBlank(jobEntity.getCronExpression())) {
            CronExpression cronExpression = new CronExpression(jobEntity.getCronExpression());
            jobEntity.setNextRunTime(cronExpression.getNextValidTimeAfter(lastExecutedTime));
        }

        HttpHeaders headers = commonService.getHttpHeaders();
        HttpEntity<JobEntity> httpEntity = new HttpEntity<>(jobEntity, headers);

        ResponseEntity<String> responseEntity = commonService.getRestTemplate().exchange(solrClientUrl + ScsbConstants.UPDATE_JOB_URL, HttpMethod.POST, httpEntity, String.class);
        return responseEntity.getBody();
    }
}
