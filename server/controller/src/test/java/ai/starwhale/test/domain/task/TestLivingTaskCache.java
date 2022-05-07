package ai.starwhale.test.domain.task;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobRuntime;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.LivingTaskCacheImpl;
import ai.starwhale.mlops.domain.task.TaskJobStatusHelper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.cmp.CMPRequest;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestLivingTaskCache {

    TaskStatusMachine taskStatusMachine = new TaskStatusMachine();

    @Test
    public void test() {
        LivingTaskCacheImpl livingTaskCache = new LivingTaskCacheImpl(null,
            null, new TaskJobStatusHelper(), taskStatusMachine, null, null);
        Job job = mockJob();
        List<Task> mockedTasks = mockTask(job);
        livingTaskCache.adopt(
            mockedTasks, TaskStatus.CREATED);
        livingTaskCache.update(mockedTasks.subList(10, 20).stream().map(Task::getId).collect(
                Collectors.toList()),
            TaskStatus.RUNNING);
        Assertions.assertEquals(246,
            livingTaskCache.ofStatus(TaskStatus.CREATED).size());
    }

    private List<Task> mockTask(Job job) {
        List<Task> of = new LinkedList<>();
        for (Long i = 1L; i < 257; i++) {
            of.add(Task.builder()
                .status(TaskStatus.CREATED)
                .id(i)
                .taskRequest(new CMPRequest("hi"))
                .job(job)
                .build());
        }
        return of;
    }

    private Job mockJob() {
        return Job.builder()
            .id(1l)
            .status(JobStatus.CREATED)
            .jobRuntime(new JobRuntime())
            .swmp(new SWModelPackage())
            .swDataSets(new ArrayList<>(0))
            .build();
    }

}
