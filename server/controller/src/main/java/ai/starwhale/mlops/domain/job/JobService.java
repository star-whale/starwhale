/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.job.ExecRequest;
import ai.starwhale.mlops.api.protocol.job.ExecResponse;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.dataset.DatasetService;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.converter.JobBoConverter;
import ai.starwhale.mlops.domain.job.converter.JobConverter;
import ai.starwhale.mlops.domain.job.po.JobFlattenEntity;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.job.split.JobSpliterator;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusCalculator;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.resulting.ResultQuerier;
import ai.starwhale.mlops.domain.job.step.task.schedule.TaskScheduler;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.model.ModelService;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.trash.Trash;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class JobService {
    private final JobConverter jobConvertor;
    private final JobBoConverter jobBoConverter;
    private final JobSpliterator jobSpliterator;
    private final JobScheduler jobScheduler;
    private final TaskScheduler taskScheduler;
    private final ResultQuerier resultQuerier;
    private final StoragePathCoordinator storagePathCoordinator;
    private final UserService userService;
    private final ProjectService projectService;
    private final JobDao jobDao;
    private final ModelService modelService;
    private final DatasetService datasetService;
    private final RuntimeService runtimeService;
    private final TrashService trashService;
    private final SystemSettingService systemSettingService;
    private final JobSpecParser jobSpecParser;

    public JobService(
            JobConverter jobConvertor, JobBoConverter jobBoConverter,
            JobSpliterator jobSpliterator, JobDao jobDao, JobScheduler jobScheduler, JobSpecParser jobSpecParser,
            ResultQuerier resultQuerier, StoragePathCoordinator storagePathCoordinator,
            ProjectService projectService, UserService userService, TrashService trashService,
            ModelService modelService, RuntimeService runtimeService, DatasetService datasetService,
            SystemSettingService systemSettingService,
            TaskScheduler taskScheduler) {
        this.jobConvertor = jobConvertor;
        this.jobBoConverter = jobBoConverter;
        this.runtimeService = runtimeService;
        this.jobSpliterator = jobSpliterator;
        this.projectService = projectService;
        this.jobDao = jobDao;
        this.jobScheduler = jobScheduler;
        this.modelService = modelService;
        this.resultQuerier = resultQuerier;
        this.datasetService = datasetService;
        this.storagePathCoordinator = storagePathCoordinator;
        this.userService = userService;
        this.trashService = trashService;
        this.systemSettingService = systemSettingService;
        this.jobSpecParser = jobSpecParser;
        this.taskScheduler = taskScheduler;
    }


    @Transactional
    public Long createJob(String projectUrl,
                          String modelVersionUrl, String datasetVersionUrls, String runtimeVersionUrl,
                          String comment, String resourcePool,
                          String handler, String stepSpecOverWrites, JobType type,
                          DevWay devWay, boolean devMode, String devPassword, Long ttlInSec) {
        User user = userService.currentUserDetail();
        String jobUuid = IdUtil.simpleUUID();
        var project = projectService.findProject(projectUrl);
        var modelVersion = modelService.findModelVersion(modelVersionUrl);
        var model = modelService.findModel(modelVersion.getModelId());

        RuntimeVersion runtimeVersion;
        if (StringUtils.hasText(runtimeVersionUrl)) {
            runtimeVersion = runtimeService.findRuntimeVersion(runtimeVersionUrl);
        } else {
            log.debug("try to find built-in runtime for model:{}", modelVersion.getId());
            runtimeVersionUrl = modelVersion.getBuiltInRuntime();
            if (!StringUtils.hasText(runtimeVersionUrl)) {
                throw new SwValidationException(ValidSubject.RUNTIME, "no runtime or built-in runtime");
            }
            runtimeVersion = runtimeService.findBuiltInRuntimeVersion(project.getId(), runtimeVersionUrl);
        }
        var runtime = runtimeService.findRuntime(runtimeVersion.getRuntimeId());

        var datasetVersionIdMaps = Arrays.stream(datasetVersionUrls.split("[,;]"))
                .map(datasetService::findDatasetVersion)
                .collect(Collectors.toMap(DatasetVersion::getId, DatasetVersion::getVersionName));

        if ((!StringUtils.hasText(stepSpecOverWrites) && !StringUtils.hasText(handler))
                || (StringUtils.hasText(stepSpecOverWrites) && StringUtils.hasText(handler))) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "handler or stepSpec must be provided only one"),
                    HttpStatus.BAD_REQUEST);
        }

        List<StepSpec> steps;
        try {
            steps = StringUtils.hasText(stepSpecOverWrites)
                    ? jobSpecParser.parseAndFlattenStepFromYaml(stepSpecOverWrites)
                    : jobSpecParser.parseStepFromYaml(modelVersion.getJobs(), handler);
            stepSpecOverWrites = Constants.yamlMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "failed to parse job step", e), HttpStatus.BAD_REQUEST);
        }

        if (CollectionUtils.isEmpty(steps)) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "no stepSpec is configured"), HttpStatus.BAD_REQUEST);
        }

        var pool = systemSettingService.queryResourcePool(resourcePool);
        if (pool != null) {
            for (var step : steps) {
                pool.validateResources(step.getResources());
            }
            if (!pool.allowUser(user.getId())) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.JOB, "user is not allowed to use this resource pool"),
                        HttpStatus.BAD_REQUEST);
            }
        }

        JobFlattenEntity jobEntity = JobFlattenEntity.builder()
                .jobUuid(jobUuid)
                .ownerId(user.getId())
                .ownerName(user.getName())
                .runtimeVersionId(runtimeVersion.getId())
                .runtimeVersionValue(runtimeVersion.getVersionName())
                .runtimeName(runtime.getName())
                .projectId(project.getId())
                .project(project)
                .modelVersionId(modelVersion.getId())
                .modelVersionValue(modelVersion.getName())
                .modelName(model.getName())
                .datasetIdVersionMap(datasetVersionIdMaps)
                .comment(comment)
                .resultOutputPath(storagePathCoordinator.allocateResultMetricsPath(jobUuid))
                .jobStatus(JobStatus.CREATED)
                .type(type)
                .resourcePool(resourcePool)
                .stepSpec(stepSpecOverWrites)
                .createdTime(new Date())
                .modifiedTime(new Date())
                .devMode(devMode)
                .devWay(devMode ? devWay : null)
                .devPassword(devMode ? devPassword : null)
                .autoReleaseTime(ttlInSec == null ? null : new Date(System.currentTimeMillis() + ttlInSec * 1000))
                .build();

        jobDao.addJob(jobEntity);
        var jobId = jobEntity.getId();
        log.info("Job has been created. ID={}", jobId);

        var job = jobDao.findJobById(jobId);

        jobSpliterator.split(job);
        // should fill steps and tasks info after split
        jobBoConverter.fillStepsAndTasks(job);
        jobScheduler.schedule(job, false);
        // this.updateJob(job);
        return jobId;
    }

    public void reloadHotJobs() {
        hotJobsFromDb().forEach(job -> {
            try {
                jobScheduler.schedule(job, false);
            } catch (Exception e) {
                log.error("loading hotting job failed {}", job.getId(), e);
                jobDao.updateJobStatus(job.getId(), JobStatus.FAIL);
            }
        });
        log.info("hot jobs loaded");
    }

    /**
     * load jobs that are not FINISHED/ERROR/CANCELED/CREATED/PAUSED into mem CREATED job has no steps yet, so it will
     * not be loaded here
     *
     * @return tasks of jobs that are not FINISHED/ERROR/CANCELED/CREATED/PAUSED
     */
    private List<Job> hotJobsFromDb() {
        List<JobStatus> hotJobStatuses = Arrays.asList(JobStatus.values())
                .parallelStream()
                .filter(JobStatusMachine::isHot)
                .collect(Collectors.toList());
        return jobDao.findJobByStatusIn(hotJobStatuses);
    }

    public PageInfo<JobVo> listJobs(String projectUrl, Long modelId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectService.getProjectId(projectUrl);
        List<Job> jobEntities = jobDao.listJobs(projectId, modelId);
        return PageUtil.toPageInfo(jobEntities, jobConvertor::convert);
    }

    public JobVo findJob(String projectUrl, String jobUrl) {
        Job job = jobDao.findJob(jobUrl);
        if (job == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, String.format("Unable to find job %s", jobUrl)),
                    HttpStatus.BAD_REQUEST);
        }

        return jobConvertor.convert(job);
    }

    public Job findJob(String jobUrl) {
        Job job = jobDao.findJob(jobUrl);
        if (job == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, String.format("Unable to find job %s", jobUrl)),
                    HttpStatus.BAD_REQUEST);
        }

        return job;
    }

    public Object getJobResult(String projectUrl, String jobUrl) {
        Long jobId = jobDao.getJobId(jobUrl);
        return resultQuerier.resultOfJob(jobId);
    }

    public void updateJob(Job job) {
        JobStatus currentStatus = job.getStatus();
        Set<StepStatus> stepStatuses = job.getSteps().stream().map(Step::getStatus)
                .collect(Collectors.toSet());
        JobStatus desiredJobStatus = JobStatusCalculator.desiredJobStatus(stepStatuses);
        if (currentStatus == desiredJobStatus) {
            log.debug("job status unchanged id:{} status:{}", job.getId(), job.getStatus());
            return;
        }
        if (!JobStatusMachine.couldTransfer(currentStatus, desiredJobStatus)) {
            log.warn("job status change unexpectedly from {} to {} of id {} ",
                    currentStatus, desiredJobStatus, job.getId());
        }
        log.info("job status change from {} to {} with id {}", currentStatus, desiredJobStatus, job.getId());
        job.setStatus(desiredJobStatus);
        jobDao.updateJobStatus(job.getId(), desiredJobStatus);

        if (JobStatusMachine.isFinal(desiredJobStatus)) {
            var finishedTime = new Date();
            var duration = finishedTime.getTime() - job.getCreatedTime().getTime();
            jobDao.updateJobFinishedTime(job.getId(), finishedTime,  duration);
            if (desiredJobStatus == JobStatus.FAIL) {
                // try to cancel other running tasks
                jobScheduler.cancel(job.getId());
            }
            jobScheduler.remove(job.getId());
        }
    }

    public Boolean updateJobComment(String projectUrl, String jobUrl, String comment) {
        return jobDao.updateJobComment(jobUrl, comment);
    }

    public Boolean updateJobPinStatus(String projectUrl, String jobUrl, Boolean pinned) {
        return jobDao.updateJobPinStatus(jobUrl, pinned);
    }

    public Boolean removeJob(String projectUrl, String jobUrl) {
        var job = jobDao.findJob(jobUrl);
        try {
            // stop schedule
            jobScheduler.cancel(job.getId());
        } catch (Exception e) {
            log.warn("Stop job schedule error, continue to remove.");
        }
        Trash trash = Trash.builder()
                .projectId(projectService.getProjectId(projectUrl))
                .objectId(job.getId())
                .type(Type.valueOf(job.getType().name()))
                .build();
        trashService.moveToRecycleBin(trash, userService.currentUserDetail());
        return jobDao.removeJob(job.getId());
    }

    public Boolean recoverJob(String projectUrl, String jobUrl) {
        throw new UnsupportedOperationException("Please use TrashService.recover() instead.");
    }

    @Transactional
    @Scheduled(initialDelay = 10, fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void gc() {
        List<Job> runningJobs = jobDao.findJobByStatusIn(List.of(JobStatus.RUNNING));
        if (CollectionUtils.isEmpty(runningJobs)) {
            log.debug("no running job");
            return;
        }
        // check if the auto release time is reached
        List<Job> jobsToRelease = runningJobs.stream()
                .filter(job -> job.getAutoReleaseTime() != null && job.getAutoReleaseTime().before(new Date()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(jobsToRelease)) {
            log.debug("no job to release");
            return;
        }
        for (Job job : jobsToRelease) {
            try {
                log.info("release job: {}", job.getId());
                cancelJob(job.getId().toString());
            } catch (Exception e) {
                log.error("failed to release job: {}", job.getId(), e);
            }
        }
    }

    /**
     * jobStatus->TO_CANCEL; RUNNING/PREPARING/ASSIGNING->CANCELLING;CREATED/PAUSED/UNKNOWN->CANCELED,READY->??
     */
    @Transactional
    public void cancelJob(String jobUrl) {
        Long jobId = jobDao.getJobId(jobUrl);
        jobScheduler.cancel(jobId);
    }

    public List<Job> listHotJobs() {
        return jobDao.findJobByStatusIn(new ArrayList<>(JobStatusMachine.HOT_JOB_STATUS));
    }

    /**
     * transactional jobStatus RUNNING->PAUSED; taskStatus CREATED->PAUSED
     */
    @Transactional
    public void pauseJob(String jobUrl) {
        Long jobId = jobDao.getJobId(jobUrl);
        jobScheduler.pause(jobId);
    }

    /**
     * jobStatus PAUSED->RUNNING; taskStatus PAUSED->CREATED jobStatus FAILED->RUNNING; taskStatus PAUSED->CREATED
     */
    public void resumeJob(String jobUrl) {
        var jobId = jobDao.getJobId(jobUrl);
        var job = jobDao.findJobById(jobId);
        if (null == job) {
            throw new SwValidationException(ValidSubject.JOB, "job not exists");
        }
        if (job.getStatus() != JobStatus.PAUSED
                && job.getStatus() != JobStatus.FAIL
                && job.getStatus() != JobStatus.CANCELED) {
            throw new SwValidationException(ValidSubject.JOB, "only failed/paused/canceled job can be resumed ");
        }
        // reschedule job
        job = jobScheduler.schedule(job, true);
        // this.updateJob(job);
    }

    public ExecResponse exec(
            String projectUrl,
            String jobUrl,
            String taskId,
            ExecRequest execRequest
    ) {
        Long jobId = jobDao.getJobId(jobUrl);
        Job job = jobDao.findJobById(jobId);
        if (null == job) {
            throw new SwValidationException(ValidSubject.JOB, "job not exists");
        }
        if (job.getStatus() != JobStatus.RUNNING) {
            throw new SwValidationException(ValidSubject.JOB, "only running job can be executed");
        }
        Task task = job.getSteps().stream()
                .map(Step::getTasks)
                .flatMap(Collection::stream)
                .filter(t -> t.getId().equals(Long.valueOf(taskId)))
                .findAny()
                .orElseThrow(() -> new SwValidationException(ValidSubject.TASK, "task not exists"));
        if (task.getStatus() != TaskStatus.RUNNING) {
            throw new SwValidationException(ValidSubject.TASK, "only running task can be executed");
        }

        try {
            var resp = taskScheduler.exec(task, execRequest.getCommand()).get();
            return ExecResponse.builder().stdout(resp[0]).stderr(resp[1]).build();
        } catch (InterruptedException | ExecutionException e) {
            throw new SwProcessException(SwProcessException.ErrorType.K8S, "exec task failed", e);
        }
    }
}
