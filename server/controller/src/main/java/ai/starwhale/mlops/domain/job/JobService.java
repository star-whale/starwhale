/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import ai.starwhale.mlops.schedule.TaskScheduler;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * deal with jobs in different status
 */
public class JobService {

    JobSpliterator jobSpliterator;

    TaskScheduler taskScheduler;

    LivingTaskStatusMachine livingTaskStatusMachine;

    /**
     * load created jobs from user at fixed delay
     */
    @Scheduled(fixedDelay = 3000)
    public void splitNewCreatedJobs(){
        final Stream<Job> allNewJobs = findAllNewJobs();
        allNewJobs.parallel().forEach(job->{
            //one transaction
            final List<TaskTrigger> taskTriggers = jobSpliterator.split(job);
            taskScheduler.adoptTasks(taskTriggers,job.getJobRuntime().getDeviceClass());
            livingTaskStatusMachine.adopt(taskTriggers.stream().map(TaskTrigger::getTask).collect(
                Collectors.toList()), TaskStatus.CREATED);
        });

    }

    Stream<Job> findAllNewJobs(){
        //TODO
        return null;
    }

    /**
     * transactional
     * jobStatus->TO_CANCEL; taskStatus->TO_CANCEL
     */
    public void cancelJob(Long jobId){
        //TODO
        livingTaskStatusMachine.adopt(null,TaskStatus.TO_CANCEL);
    }




}
