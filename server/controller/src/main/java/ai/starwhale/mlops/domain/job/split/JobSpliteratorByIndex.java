/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job.split;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swds.index.SWDSBlock;
import ai.starwhale.mlops.domain.swds.index.SWDSIndex;
import ai.starwhale.mlops.domain.swds.index.SWDSIndexLoader;
import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * split job by swds index
 */
public class JobSpliteratorByIndex implements JobSpliterator {

    SWDSIndexLoader swdsIndexLoader;

    /**
     * get all data blocks and split them by a simple random number
     * transactional jobStatus->SPLIT taskStatus->NEW
     */
    @Override
    public List<TaskTrigger> split(Job job) {
        final List<SWDataSet> swDataSets = job.getSwDataSets();
        Integer deviceAmount = job.getJobRuntime().getDeviceAmount();
        Random r = new Random();
        final Map<Integer,List<SWDSBlock>> swdsBlocks = swDataSets.parallelStream()
            .map(swDataSet -> swdsIndexLoader.load(swDataSet.getIndexPath()))
            .map(SWDSIndex::getSWDSBlockList)
            .flatMap(Collection::stream)
            .collect(Collectors.groupingBy(blk->r.nextInt(deviceAmount)))
            ;
        final List<Task> tasks = allocateTasks(job, deviceAmount);
        return squashTaskTriggers(job, tasks, swdsBlocks);
    }

    private List<TaskTrigger> squashTaskTriggers(Job job, List<Task> tasks, Map<Integer, List<SWDSBlock>> swdsBlocks) {
        List<TaskTrigger> taskTriggers = new LinkedList<>();
        for(int i=0;i<job.getJobRuntime().getDeviceAmount();i++){
            taskTriggers.add(TaskTrigger.builder()
                .swModelPackage(job.getSwmp())
                .imageId(job.getJobRuntime().getBaseImage())
                .swdsBlocks(swdsBlocks.get(i))
                .task(tasks.get(i))
                .build());
        }
        return taskTriggers;
    }

    private List<Task> allocateTasks(Job job, Integer deviceAmount) {
        //TODO tasks should be saved into DB in this step, here is a mock,if tasks already exist load them

        List<Task> tasks = new LinkedList<>();
        for (int i = 0; i < deviceAmount; i++) {
            tasks.add(Task.builder().jobId(job.getId()).build());
        }

        job.setStatus(JobStatus.SPLIT);
        return tasks;
    }
}
