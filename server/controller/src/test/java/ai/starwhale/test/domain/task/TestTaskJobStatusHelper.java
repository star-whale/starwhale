/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.test.domain.task;

import static ai.starwhale.mlops.domain.task.TaskType.*;

import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.task.TaskJobStatusHelper;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;

import static ai.starwhale.mlops.domain.task.status.TaskStatus.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestTaskJobStatusHelper {

    TaskJobStatusHelper taskJobStatusHelper = new TaskJobStatusHelper();

    @Test
    public void testToCollect() {
        List<Task> tasks = List.of(successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        JobStatus toCollectResult = JobStatus.TO_COLLECT_RESULT;
        Assertions.assertEquals(toCollectResult, taskJobStatusHelper.desiredJobStatus(tasks));
    }

    @Test
    public void testCollecting() {
        List<Task> tasks = List.of(
            mock(CMP, CREATED), successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        logTasks(tasks);
        JobStatus collectingResult = JobStatus.COLLECTING_RESULT;
        Assertions.assertEquals(collectingResult, taskJobStatusHelper.desiredJobStatus(tasks));
        tasks = List.of(
            mock(CMP, ASSIGNING), successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        logTasks(tasks);
        Assertions.assertEquals(collectingResult, taskJobStatusHelper.desiredJobStatus(tasks));
        tasks = List.of(
            mock(CMP, PREPARING), successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        logTasks(tasks);
        Assertions.assertEquals(collectingResult, taskJobStatusHelper.desiredJobStatus(tasks));
        tasks = List.of(
            mock(CMP, RUNNING), successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        logTasks(tasks);
        Assertions.assertEquals(collectingResult, taskJobStatusHelper.desiredJobStatus(tasks));

    }

    private void logTasks(List<Task> tasks) {
        log.debug("evaluating {}", toStr(tasks));
    }

    @Test
    public void testSuccess() {
        List<Task> tasks = List.of(
            mock(CMP, SUCCESS), successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        logTasks(tasks);
        JobStatus success = JobStatus.SUCCESS;
        Assertions.assertEquals(success, taskJobStatusHelper.desiredJobStatus(
            tasks));

    }

    @Test
    public void testCancelling() {
        List<Task> tasks = List.of(
            mock(CMP, CANCELLING), successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        logTasks(tasks);
        JobStatus canceling = JobStatus.CANCELING;
        Assertions.assertEquals(canceling, taskJobStatusHelper.desiredJobStatus(tasks));

        tasks = List.of(
            mock(CMP, TO_CANCEL), successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        Assertions.assertEquals(canceling, taskJobStatusHelper.desiredJobStatus(tasks));

        tasks = List.of(
            mock(PPL, TO_CANCEL), successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        Assertions.assertEquals(canceling, taskJobStatusHelper.desiredJobStatus(tasks));

        List<Task> tasks1 = List.of(
            mock(PPL, CANCELLING), mock(PPL, CANCELLING), successPPL(), successPPL(), successPPL(),
            successPPL(), successPPL());
        logTasks(tasks1);
        Assertions.assertEquals(canceling, taskJobStatusHelper.desiredJobStatus(tasks1));
        List<Task> tasks2 = List.of(
            mock(PPL, CANCELLING), mock(PPL, TO_CANCEL), mock(PPL, CANCELED), successPPL(),
            successPPL(), successPPL(), successPPL(), successPPL());
        logTasks(tasks2);
        Assertions.assertEquals(canceling, taskJobStatusHelper.desiredJobStatus(tasks2));


    }

    @Test
    public void testCancelled() {
        List<Task> tasks = List.of(
            mock(CMP, CANCELED), successPPL(), successPPL(), successPPL(), successPPL(),
            successPPL());
        logTasks(tasks);
        JobStatus canceled = JobStatus.CANCELED;
        Assertions.assertEquals(canceled, taskJobStatusHelper.desiredJobStatus(tasks));
        List<Task> tasks1 = List.of(mock(PPL, CANCELED), mock(PPL, CANCELED), successPPL(),
            successPPL(), successPPL(), successPPL(), successPPL());
        logTasks(tasks1);
        Assertions.assertEquals(canceled, taskJobStatusHelper.desiredJobStatus(tasks1));

    }

    @Test
    public void testRunning() {
        List<Task> tasks = List.of(
            mock(PPL, RUNNING),
            mock(PPL, PREPARING),
            mock(PPL, ASSIGNING),
            successPPL(), successPPL(), successPPL(), successPPL());
        logTasks(tasks);
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            tasks));

        tasks = List.of(
            mock(PPL, CREATED),
            mock(PPL, CREATED),
            mock(PPL, CREATED));
        logTasks(tasks);
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            tasks));

        tasks = List.of(
            mock(PPL, ASSIGNING),
            mock(PPL, ASSIGNING),
            mock(PPL, CREATED),
            mock(PPL, CREATED),
            mock(PPL, CREATED),
            mock(PPL, CREATED));
        logTasks(tasks);
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            tasks));

        List<Task> tasks1 = List.of(
            mock(PPL, RUNNING),
            mock(PPL, RUNNING), successPPL(), successPPL(), successPPL(), successPPL());
        logTasks(tasks1);
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            tasks1));
        List<Task> tasks2 = List.of(
            mock(PPL, PREPARING),
            mock(PPL, PREPARING), successPPL(), successPPL(), successPPL(), successPPL());
        logTasks(tasks2);
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            tasks2));
        List<Task> tasks3 = List.of(
            mock(PPL, ASSIGNING),
            mock(PPL, ASSIGNING), successPPL(), successPPL(), successPPL(), successPPL());
        logTasks(tasks3);
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            tasks3));

    }

    @Test
    public void testFail() {
        List<Task> tasks = List.of(
            mock(CMP, FAIL), successPPL(), successPPL(), successPPL(), successPPL(), successPPL());
        logTasks(tasks);
        Assertions.assertEquals(JobStatus.FAIL, taskJobStatusHelper.desiredJobStatus(
            tasks));
        List<Task> tasks1 = List.of(
            mock(PPL, FAIL), mock(PPL, CANCELED), successPPL(), successPPL(), successPPL(),
            successPPL(), successPPL());
        logTasks(tasks1);
        Assertions.assertEquals(JobStatus.FAIL, taskJobStatusHelper.desiredJobStatus(
            tasks1));

    }

    private Task successPPL() {
        return mock(PPL, SUCCESS);
    }

    private Task mock(TaskType tt, TaskStatus ts) {
        return Task.builder().taskType(tt).status(ts).build();
    }

    String toStr(Collection<Task> tasks) {
        return String.join(",", tasks.parallelStream()
            .map(t -> t.getTaskType().toString() + " " + t.getStatus().toString()).collect(
                Collectors.toList()));
    }

}
