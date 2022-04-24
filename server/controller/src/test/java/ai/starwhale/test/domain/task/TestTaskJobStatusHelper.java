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
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTaskJobStatusHelper {

    TaskJobStatusHelper taskJobStatusHelper = new TaskJobStatusHelper();

    @Test
    public void testToCollect(){
        List<Task> tasks = List.of(successPPL(),successPPL(),successPPL(),successPPL(),successPPL());
        Assertions.assertEquals(JobStatus.TO_COLLECT_RESULT,taskJobStatusHelper.desiredJobStatus(tasks));
    }

    @Test
    public void testCollecting(){
        Assertions.assertEquals(JobStatus.COLLECTING_RESULT, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(CMP, CREATED))));
        Assertions.assertEquals(JobStatus.COLLECTING_RESULT, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(CMP, ASSIGNING))));
        Assertions.assertEquals(JobStatus.COLLECTING_RESULT, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(CMP, PREPARING))));
        Assertions.assertEquals(JobStatus.COLLECTING_RESULT, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(CMP, RUNNING))));

    }

    @Test
    public void testSuccess(){
        Assertions.assertEquals(JobStatus.SUCCESS, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(CMP, SUCCESS))));

    }

    @Test
    public void testCancelling(){
        Assertions.assertEquals(JobStatus.CANCELING, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(CMP, CANCELLING))));
        Assertions.assertEquals(JobStatus.CANCELING, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(PPL, CANCELLING),mock(PPL, CANCELLING))));
        Assertions.assertEquals(JobStatus.CANCELING, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(PPL, CANCELLING),mock(PPL, TO_CANCEL),mock(PPL, CANCELED))));


    }

    @Test
    public void testCancelled(){
        Assertions.assertEquals(JobStatus.CANCELED, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(CMP, CANCELED))));
        Assertions.assertEquals(JobStatus.CANCELED, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(PPL, CANCELED),mock(PPL, CANCELED))));

    }

    @Test
    public void testRunning(){
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), mock(PPL, RUNNING),
                mock(PPL, PREPARING),mock(PPL, ASSIGNING))));
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), mock(PPL, RUNNING),
                mock(PPL, RUNNING))));
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), mock(PPL, PREPARING),
                mock(PPL, PREPARING))));
        Assertions.assertEquals(JobStatus.RUNNING, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), mock(PPL, ASSIGNING),
                mock(PPL, ASSIGNING))));

    }

    @Test
    public void testFail(){
        Assertions.assertEquals(JobStatus.FAIL, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(CMP, FAIL))));
        Assertions.assertEquals(JobStatus.FAIL, taskJobStatusHelper.desiredJobStatus(
            List.of(successPPL(), successPPL(), successPPL(), successPPL(), successPPL(),
                mock(PPL, FAIL),mock(PPL, CANCELED))));

    }

    private Task successPPL() {
        return mock(PPL,SUCCESS);
    }

    private Task successCMP() {
        return Task.builder().taskType(CMP).status(SUCCESS).build();
    }

    private Task mock(TaskType tt, TaskStatus ts){
        return Task.builder().taskType(tt).status(ts).build();
    }

}
