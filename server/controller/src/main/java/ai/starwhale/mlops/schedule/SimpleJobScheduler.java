/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.schedule;

import static ai.starwhale.mlops.domain.job.Job.JobStatus.CREATED;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.JobSpliter;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * a simple implementation of JobScheduler
 */
@Slf4j
public class SimpleJobScheduler implements JobScheduler {

    JobSpliter jobSpliter;

    final Map<Device.Clazz, ConcurrentLinkedQueue<TaskTrigger>> taskQueueTable;

    public SimpleJobScheduler(JobSpliter jobSpliter) {
        this.jobSpliter = jobSpliter;
        Map<Device.Clazz, ConcurrentLinkedQueue<TaskTrigger>> taskQueueTableInitialization = new HashMap<>();
        taskQueueTableInitialization.put(Clazz.CPU, new ConcurrentLinkedQueue<>());
        taskQueueTableInitialization.put(Clazz.GPU, new ConcurrentLinkedQueue<>());
        this.taskQueueTable = Collections.unmodifiableMap(taskQueueTableInitialization);
    }

    @Override
    public void takeJob(Job job) {
        validJob(job);
        List<TaskTrigger> splitTasks = jobSpliter.split(job);
        taskQueueTable.get(job.getJobRuntime().getDeviceClass()).addAll(splitTasks);
        job.setStatus(JobStatus.SPLIT);
        //save job status TODO
    }

    @Override
    public List<TaskTrigger> schedule(Node node) {
        validNode(node);
        return node.getDeviceHolders()
            .stream()
            .filter(deviceHolder -> null == deviceHolder.getHolder()) //only schedule devices that is free
            .map(deviceHolder -> taskQueueTable.get(deviceHolder.getDevice().getClazz()).poll())// pull task from the device corresponding queue
            .filter(Objects::nonNull)//remove null tasks got from empty queue
            .collect(Collectors.toList());
    }

    private void validNode(Node node) {
        //assuming that all DeviceHolders are valid
        if (null == node || null == node.getDeviceHolders()) {
            log.error("bad node scheduled, null or null field");
            throw new SWValidationException(ValidSubject.NODE);
        }
    }

    public void validJob(Job job) {
        if (null == job
            || null == job.getStatus()
            || null == job.getJobRuntime()
            || null == job.getJobRuntime().getDeviceClass()) {
            log.error("bad job taken, null or null field");
            throw new SWValidationException(ValidSubject.JOB);
        }
        if (CREATED != job.getStatus()) {
            log.error("only CREATED status job shall be scheduled but the status is {}",
                job.getStatus());
            throw new SWValidationException(ValidSubject.JOB);
        }
    }
}
