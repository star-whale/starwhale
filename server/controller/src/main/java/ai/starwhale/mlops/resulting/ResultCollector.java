package ai.starwhale.mlops.resulting;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.task.LivingTaskStatusMachine;
import ai.starwhale.mlops.domain.task.TaskStatus;
import ai.starwhale.mlops.domain.task.bo.StagingTaskStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskStatusStage;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.resulting.pipline.ResultingPPL;
import ai.starwhale.mlops.resulting.pipline.ResultingPPLFinder;
import ai.starwhale.mlops.resulting.repo.IndicatorRepo;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class ResultCollector {

    final JobBoConverter jobBoConverter;

    final ResultingPPLFinder resultingPPLFinder;

    final JobMapper jobMapper;

    final LivingTaskStatusMachine livingTaskStatusMachine;

    final StorageAccessService storageAccessService;

    public ResultCollector(JobBoConverter jobBoConverter,
        ResultingPPLFinder resultingPPLFinder,
        JobMapper jobMapper,
        LivingTaskStatusMachine livingTaskStatusMachine,
        StorageAccessService storageAccessService) {
        this.jobBoConverter = jobBoConverter;
        this.resultingPPLFinder = resultingPPLFinder;
        this.jobMapper = jobMapper;
        this.livingTaskStatusMachine = livingTaskStatusMachine;
        this.storageAccessService = storageAccessService;
    }

    @Scheduled(fixedDelay = 1000*10)
    public void onJobCollect(){
        List<JobEntity> collectJobs = jobMapper.findJobByStatusIn(
            List.of(JobStatus.COLLECT_RESULT.getValue()));
        collectJobs.parallelStream().forEach(jobEntity -> {
            Job job = jobBoConverter.fromEntity(jobEntity);
            ResultingPPL resultingPPL = resultingPPLFinder.findForJob(job);
            final IndicatorRepo indicatorRepo = resultingPPL.getIndicatorRepo();
            List<Indicator> taskLevelIndicators = livingTaskStatusMachine.ofJob(job.getId()).parallelStream()
                .map(Task::getUuid)
                .map(uuid -> {
                    try {
                        return indicatorRepo.loadTaskLevel(uuid);
                    } catch (IOException e) {
                        log.error("load task ", e);
                        return new ArrayList<Indicator>();
                    }
                }).flatMap(Collection::stream)
                .collect(Collectors.toList());
            Collection<Indicator> jobLevelIndicators = resultingPPL.getJobResultAggregator()
                .aggregate(taskLevelIndicators);
            Collection<Indicator> uiLevelIndicators = resultingPPL.getJobResultCalculator()
                .calculate(jobLevelIndicators);
            jobMapper.updateJobStatus(List.of(job.getId()),JobStatus.FINISHED.getValue());
            String jobUuid = job.getUuid();
            try {
                indicatorRepo.saveJobLevel(jobLevelIndicators, jobUuid);
                indicatorRepo.saveUILevel(uiLevelIndicators, jobUuid);
            } catch (IOException e) {
                log.error("save job level indicator or ui level indicator failed for {}",job.getId(),e);
                throw new SWProcessException(ErrorType.STORAGE);
            }
        });
    }


    @Scheduled(fixedDelay = 1000)
    public void onTaskFinished() {
        final Collection<Task> finishedTasks = livingTaskStatusMachine.ofStatus(
            new StagingTaskStatus(
                TaskStatus.FINISHED));
        if (CollectionUtils.isEmpty(finishedTasks)) {
            log.debug("no finished task");
            return;
        }
        livingTaskStatusMachine.update(finishedTasks, new StagingTaskStatus(TaskStatus.FINISHED,
            TaskStatusStage.DOING));
        finishedTasks.parallelStream()
            .forEach(finishedTask -> {
                ResultingPPL resultingPPL = resultingPPLFinder.findForJob(finishedTask.getJob());
                final String resultLabelPath = finishedTask.getResultPaths();
                final Stream<String> resultLabels;
                Long taskId = finishedTask.getId();
                try {
                    resultLabels = storageAccessService.list(resultLabelPath);
                } catch (IOException e) {
                    log.error("listing inference results for task failed {}", taskId, e);
                    livingTaskStatusMachine.update(List.of(finishedTask),
                        new StagingTaskStatus(TaskStatus.FINISHED, TaskStatusStage.INIT));
                    return;
                }
                List<Indicator> allDataLevelIndicators = resultLabels.map((labelComparePath) -> {
                        try (final InputStream labelIS = storageAccessService.get(labelComparePath)) {
                            List<Indicator> calculate = resultingPPL.getDataResultCalculator()
                                .calculate(labelIS);
                            return calculate;
                        } catch (JsonProcessingException jsonProcessingException) {
                            log.error("reading labels failed for task {}", taskId,
                                jsonProcessingException);
                            livingTaskStatusMachine.update(List.of(finishedTask),
                                new StagingTaskStatus(TaskStatus.FINISHED, TaskStatusStage.FAILED));
                            return null;
                        } catch (IOException e) {
                            log.error("reading labels failed for task {}", taskId, e);
                            livingTaskStatusMachine.update(List.of(finishedTask),
                                new StagingTaskStatus(TaskStatus.FINISHED, TaskStatusStage.INIT));
                            return null;
                        }
                    }).filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
                Collection<Indicator> taskLevelIndicators = resultingPPL.getTaskResultAggregator()
                    .aggregate(allDataLevelIndicators);
                try {
                    resultingPPL.getIndicatorRepo().saveTaskLevel(taskLevelIndicators,
                        finishedTask.getUuid());
                    livingTaskStatusMachine.update(finishedTasks,
                        new StagingTaskStatus(TaskStatus.FINISHED, TaskStatusStage.DONE));
                } catch (JsonProcessingException jsonProcessingException) {
                    log.error("saving task level result failed {}", taskId,
                        jsonProcessingException);
                    livingTaskStatusMachine.update(List.of(finishedTask),
                        new StagingTaskStatus(TaskStatus.FINISHED, TaskStatusStage.FAILED));
                } catch (IOException e) {
                    log.error("saving task level result failed {}", taskId, e);
                    livingTaskStatusMachine.update(List.of(finishedTask),
                        new StagingTaskStatus(TaskStatus.FINISHED, TaskStatusStage.INIT));
                }
            });
    }
}
