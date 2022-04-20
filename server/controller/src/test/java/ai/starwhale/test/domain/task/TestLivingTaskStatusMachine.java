package ai.starwhale.test.domain.task;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.JobRuntime;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachineImpl;
import ai.starwhale.mlops.domain.task.TaskJobStatusHelper;
import ai.starwhale.mlops.domain.task.TaskStatus;
import ai.starwhale.mlops.domain.task.bo.StagingTaskStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.cmp.CMPRequest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestLivingTaskStatusMachine {

    @Test
    public void test() {
        LivingTaskStatusMachineImpl livingTaskStatusMachine = new LivingTaskStatusMachineImpl(null,
            null, new TaskJobStatusHelper());
        Job job = mockJob();
        List<Task> mockedTasks = mockTask(job);
        livingTaskStatusMachine.adopt(
            mockedTasks, new StagingTaskStatus(TaskStatus.CREATED));
        livingTaskStatusMachine.update(mockedTasks.subList(10, 20),
            new StagingTaskStatus(TaskStatus.RUNNING));
        Assertions.assertEquals(246,
            livingTaskStatusMachine.ofStatus(new StagingTaskStatus(TaskStatus.CREATED)).size());
    }

    private List<Task> mockTask(Job job) {
        List<Task> of = new LinkedList<>();
        for (Long i = 1L; i < 257; i++) {
            of.add(Task.builder()
                .status(new StagingTaskStatus(TaskStatus.CREATED))
                .id(i)
                .taskRequest(new CMPRequest())
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
