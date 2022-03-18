/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.job;

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
        List<List<Integer>> slicePointsPerDS = swDataSets.stream()
            .map(ds -> slicePoints(ds.getSize(), deviceAmount))
            .collect(Collectors.toList());
        List<TaskTrigger> taskTriggers = new ArrayList<>(getInitialCapacity(deviceAmount));
        for (int i = 0; i < deviceAmount; i++) {
            int swdsSize = swDataSets.size();
            List<SWDataSetSlice> swDataSetSlices = new ArrayList<>(getInitialCapacity(
                swdsSize));
            for (int j = 0; j < swdsSize; j++) {
                List<Integer> slicePoints = slicePointsPerDS.get(j);
                boolean lastSlice = i == deviceAmount - 1;
                SWDataSetSlice swdsSlice = SWDataSetSlice.builder()
                    .swDataSet(swDataSets.get(j))
                    .start(slicePoints.get(i))
                    .end(lastSlice ? slicePoints.get(i + 1) : slicePoints.get(i + 1) - 1)
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

    private int getInitialCapacity(Integer expectedSize) {
        return Double.valueOf(Math.ceil((expectedSize) * 1.4)).intValue();
    }

    private List<Task> allocateTasks(Long id, Integer deviceAmount) {
        //TODO tasks should be saved into DB in this step, here is a mock

        List<Task> tasks = new LinkedList<>();
        for (int i = 0; i < deviceAmount; i++) {
            tasks.add(Task.builder().jobId(id).id(1L).build());
        }
        return tasks;
    }

    /**
     * (8,3) -> (0 3 6 7); (8,4) -> (0 2 4 6 7); (3,4) -> (0 1 2); slicePoints.size() == sliceAmount +
     * 1  || slicePoints.size() == total + 1
     */
    private List<Integer> slicePoints(final Integer total, final Integer sliceAmount) {
        if (sliceAmount <= 0) {
            log.error("invalid slice amount expected greater than 0");
            throw new SWValidationException(ValidSubject.JOB);
        }
        //if sliceAmount > total ,sliceSize is 1 and eventually we got (total + 1) slicePoints instead of (sliceAmount + 1)
        final int sliceSizeRound = total / sliceAmount;
        final int sliceSize = total % sliceAmount == 0 ? sliceSizeRound : sliceSizeRound + 1;
        int slicePoint = 0;
        List<Integer> slicePoints = new ArrayList<>(getInitialCapacity(sliceAmount + 1));
        do {
            slicePoints.add(slicePoint);
            slicePoint += sliceSize;
        } while (slicePoint < total - 1);
        slicePoints.add(total - 1);
        return slicePoints;
    }


}
