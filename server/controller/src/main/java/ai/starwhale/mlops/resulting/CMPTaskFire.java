package ai.starwhale.mlops.resulting;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.bo.cmp.CMPRequest;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * fire a CMPTask to Agent to run
 */
@Slf4j
@Service
public class CMPTaskFire {

    final JobBoConverter jobBoConverter;

    final TaskMapper taskMapper;

    final JobMapper jobMapper;

    final LivingTaskStatusMachine livingTaskStatusMachine;

    final StorageAccessService storageAccessService;

    final TaskBoConverter taskBoConverter;

    final SWTaskScheduler swTaskScheduler;

    public CMPTaskFire(JobBoConverter jobBoConverter,
        TaskMapper taskMapper,
        JobMapper jobMapper,
        LivingTaskStatusMachine livingTaskStatusMachine,
        StorageAccessService storageAccessService,
        TaskBoConverter taskBoConverter,
        SWTaskScheduler swTaskScheduler) {
        this.jobBoConverter = jobBoConverter;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.livingTaskStatusMachine = livingTaskStatusMachine;
        this.storageAccessService = storageAccessService;
        this.taskBoConverter = taskBoConverter;
        this.swTaskScheduler = swTaskScheduler;
    }

    @Transactional
    @Scheduled(fixedDelay = 1000*10)
    public void onJobCollect(){
        jobMapper.findJobByStatusIn(List.of(JobStatus.TO_COLLECT_RESULT))
            .parallelStream()
            .forEach(jobEntity -> dispatchCMPTask(jobBoConverter.fromEntity(jobEntity)));
    }

    private void dispatchCMPTask(Job job) {
        Collection<Task> tasks = livingTaskStatusMachine.ofJob(job.getId());
        List<String> allPPLTaskResults = tasks.parallelStream().flatMap(task -> {
                try {
                    return storageAccessService.list(task.getResultDir());
                } catch (IOException e) {
                    throw new SWProcessException(ErrorType.STORAGE).tip("list task result dir failed");
                }
            })
            .collect(Collectors.toList());

        TaskEntity taskEntity = TaskEntity.builder()
            .jobId(job.getId())
            .taskRequest(new CMPRequest(allPPLTaskResults).toString())
            .taskType(TaskType.CMP)
            .resultPath(job.getResultDir())
            .taskStatus(TaskStatus.CREATED)
            .taskUuid(UUID.randomUUID().toString())
            .build();
        taskMapper.addTask(taskEntity);
        jobMapper.updateJobStatus(List.of(job.getId()),JobStatus.COLLECTING_RESULT);

        List<Task> cmpTasks = taskBoConverter.fromTaskEntity(List.of(taskEntity), job);
        swTaskScheduler.adoptTasks(cmpTasks, Clazz.CPU);
    }

}
