/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.test.domain.task;

import static ai.starwhale.mlops.domain.task.TaskType.CMP;
import static ai.starwhale.mlops.domain.task.TaskType.PPL;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.ASSIGNING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELLING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CREATED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.FAIL;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.PAUSED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.PREPARING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.RUNNING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.SUCCESS;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.TO_CANCEL;

import ai.starwhale.mlops.domain.job.status.TaskStatusRequirement;
import ai.starwhale.mlops.domain.job.status.TaskStatusRequirement.RequireType;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTaskStatusRequirement {

    @Test
    public void testAny(){
        TaskStatusRequirement trAny = new TaskStatusRequirement(
            Set.of(FAIL), null, RequireType.ANY);
        Assertions.assertTrue(trAny.fit(List.of(successPPL(),successPPL(),successPPL(),mock(PPL,FAIL))));
        Assertions.assertTrue(!trAny.fit(List.of(successPPL(),successPPL(),mock(PPL,RUNNING))));
        Assertions.assertTrue(trAny.fit(List.of(successPPL(),successPPL(),successPPL(),successPPL(),mock(CMP,FAIL))));
    }

    @Test
    public void testAll(){
        TaskStatusRequirement tr = new TaskStatusRequirement(Set.of(SUCCESS), TaskType.PPL, RequireType.ALL);
        TaskStatusRequirement tr2 = new TaskStatusRequirement(
            Set.of(CREATED), TaskType.PPL, RequireType.ALL);
        Assertions.assertTrue(tr.fit(List.of(successPPL(),successPPL(),successPPL())));
        Assertions.assertTrue(!tr.fit(List.of(successPPL(),successPPL(),mock(PPL,RUNNING))));
        Assertions.assertTrue(tr.fit(List.of(successPPL(),successPPL(),successPPL(),successPPL(),mock(CMP,RUNNING))));

        tr = new TaskStatusRequirement(Set.of(SUCCESS), TaskType.CMP, RequireType.ALL);
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(CMP,SUCCESS))));

        Assertions.assertTrue(tr2.fit(List.of(mock(PPL,CREATED),mock(PPL,CREATED),mock(PPL,CREATED))));
        Assertions.assertTrue(tr2.fit(List.of(mock(PPL,CREATED),mock(PPL,CREATED),mock(PPL,CREATED),mock(PPL,CREATED),mock(CMP,RUNNING))));
        Assertions.assertTrue(!tr2.fit(List.of(mock(PPL,CREATED),mock(PPL,CREATED),mock(PPL,CREATED),mock(PPL,CREATED),mock(PPL,RUNNING))));
    }

    @Test
    public void testMust(){
        TaskStatusRequirement tr = new TaskStatusRequirement(
            Set.of(PAUSED), null, RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(CMP,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(!tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));

        tr = new TaskStatusRequirement(Set.of(CANCELED), null, RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,CANCELED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(CMP,CANCELED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(!tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));

        tr = new TaskStatusRequirement(Set.of(CANCELLING), null, RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(CMP,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(!tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(PPL,SUCCESS),mock(PPL,FAIL))));

        tr = new TaskStatusRequirement(Set.of(SUCCESS), TaskType.CMP, RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(CMP,SUCCESS))));
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(CMP,SUCCESS))));

        tr = new TaskStatusRequirement(
            Set.of(CREATED, ASSIGNING, PREPARING, RUNNING),
            TaskType.CMP, RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,CREATED),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,ASSIGNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,PREPARING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));
        Assertions.assertTrue(!tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,FAIL))));

        tr = new TaskStatusRequirement(
            Set.of(ASSIGNING, PREPARING, RUNNING), TaskType.PPL,
            RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,RUNNING))));
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,PREPARING))));
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,ASSIGNING))));
    }

    @Test
    public void testHaveNo(){
        TaskStatusRequirement tr = new TaskStatusRequirement(Set.of(FAIL), null,
            RequireType.HAVE_NO);
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,RUNNING))));

        tr = new TaskStatusRequirement(
            Set.of(TaskStatus.PAUSED, TO_CANCEL, TaskStatus.CANCELLING,
                TaskStatus.CANCELED, TaskStatus.FAIL), TaskType.PPL, RequireType.HAVE_NO);
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(PPL,SUCCESS),mock(PPL,RUNNING))));
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,RUNNING))));

        tr = new TaskStatusRequirement(Set.of(TaskStatus.values()), TaskType.CMP,
            RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(PPL,SUCCESS),mock(PPL,RUNNING))));
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,RUNNING))));

        tr = new TaskStatusRequirement(Set.of(TaskStatus.SUCCESS), TaskType.CMP, RequireType.HAVE_NO);
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,PAUSED),mock(PPL,CANCELLING),mock(PPL,SUCCESS),mock(PPL,RUNNING))));
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(PPL,SUCCESS),mock(CMP,RUNNING))));
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(CMP,SUCCESS),mock(CMP,RUNNING))));

        tr = new TaskStatusRequirement(
            Set.of(TaskStatus.FAIL, TO_CANCEL, TaskStatus.CANCELLING,
                TaskStatus.CANCELED), null, RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(CMP,SUCCESS),mock(CMP,FAIL))));
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(CMP,SUCCESS),mock(CMP,TO_CANCEL))));
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(CMP,SUCCESS),mock(CMP,CANCELED))));
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(CMP,SUCCESS),mock(PPL,CANCELED))));
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(CMP,SUCCESS),mock(CMP,CANCELED))));
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,PREPARING),mock(PPL,SUCCESS),mock(CMP,CREATED))));
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(CMP,PAUSED),mock(CMP,PREPARING),mock(PPL,SUCCESS),mock(CMP,CREATED))));

        tr = new TaskStatusRequirement(Set.of(TaskStatus.FAIL), null, RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(CMP,SUCCESS),mock(CMP,FAIL))));
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(CMP,PAUSED),mock(CMP,CANCELLING),mock(CMP,SUCCESS),mock(CMP,SUCCESS))));

        tr = new TaskStatusRequirement(
            Set.of(TaskStatus.FAIL, TaskStatus.CANCELLING, TO_CANCEL,
                TaskStatus.CREATED, TaskStatus.ASSIGNING, TaskStatus.PAUSED,
                TaskStatus.PREPARING, TaskStatus.RUNNING), null, RequireType.HAVE_NO);
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,CANCELED),mock(PPL,SUCCESS),mock(PPL,SUCCESS))));
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,SUCCESS),mock(PPL,PREPARING))));

        tr = new TaskStatusRequirement(Set.of(TaskStatus.CREATED), null, RequireType.HAVE_NO);
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,SUCCESS),mock(PPL,PREPARING))));
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,SUCCESS),mock(PPL,CREATED))));

        tr = new TaskStatusRequirement(
            Set.of(TO_CANCEL, TaskStatus.CANCELLING, TaskStatus.CANCELED,
                TaskStatus.FAIL), null, RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,TO_CANCEL),mock(PPL,CREATED))));
        Assertions.assertTrue(tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,SUCCESS),mock(PPL,CREATED))));

        tr = new TaskStatusRequirement(Set.of(TaskStatus.values()), TaskType.CMP,
            RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(mock(CMP,RUNNING),mock(PPL,SUCCESS),mock(PPL,CREATED))));
        Assertions.assertTrue(tr.fit(List.of(mock(PPL,RUNNING),mock(PPL,SUCCESS),mock(PPL,CREATED))));
    }

    private Task successPPL() {
        return mock(PPL,SUCCESS);
    }

    private Task mock(TaskType tt, TaskStatus ts){
        return Task.builder().taskType(tt).status(ts).build();
    }

}


