/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.common.util.ArrayHelper;
import ai.starwhale.mlops.common.util.ArrayHelper.Segment;
import ai.starwhale.mlops.domain.swds.SWDataSet;
import ai.starwhale.mlops.domain.swds.SWDataSetSlice;
import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleJobSpliter implements JobSpliter {

    @Override
    public List<TaskTrigger> split(Job job) {
        Integer deviceAmount = job.getJobRuntime().getDeviceAmount();
        //allocatedTasks.size() == deviceAmount
        List<Task> allocatedTasks = allocateTasks(job.getId(), deviceAmount);
        List<SWDataSet> swDataSets = job.getSwDataSets();
        //slicePointsPerDS.size() == swDataSets.size()
        List<List<Segment>> slicePointsPerDS = swDataSets.stream()
            .map(ds -> ArrayHelper.INSTANCE.sliceSegments(ds.getSize(), deviceAmount))
            .collect(Collectors.toList());
        List<TaskTrigger> taskTriggers = new ArrayList<>(ArrayHelper.INSTANCE.getInitialCapacity(deviceAmount));
        for (int i = 0; i < deviceAmount; i++) {
            int swdsSize = swDataSets.size();
            List<SWDataSetSlice> swDataSetSlices = new ArrayList<>(ArrayHelper.INSTANCE.getInitialCapacity(
                swdsSize));
            for (int j = 0; j < swdsSize; j++) {
                List<Segment> sliceSegments = slicePointsPerDS.get(j);
                SWDataSetSlice swdsSlice = SWDataSetSlice.builder()
                    .swDataSet(swDataSets.get(j))
                    .start(sliceSegments.get(i).getStartInclusive())
                    .end(sliceSegments.get(i).getEndInclusive())
                    .build();
                swDataSetSlices.add(swdsSlice);
            }
            TaskTrigger taskTrigger = TaskTrigger.builder().task(allocatedTasks.get(i))
                .swModelPackage(job.getSwmp())
                .swDataSetSlice(swDataSetSlices)
                .build();
            taskTriggers.add(taskTrigger);
        }
        return taskTriggers;
    }


    private List<Task> allocateTasks(Long id, Integer deviceAmount) {
        //TODO tasks should be saved into DB in this step, here is a mock

        List<Task> tasks = new LinkedList<>();
        for (int i = 0; i < deviceAmount; i++) {
            tasks.add(Task.builder().jobId(id).id(1L).build());
        }
        return tasks;
    }

}
