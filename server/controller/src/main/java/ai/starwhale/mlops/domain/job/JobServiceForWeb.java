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
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.converter.JobConverter;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.job.status.JobUpdateHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForPersist;
import ai.starwhale.mlops.domain.trash.Trash;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.resulting.ResultQuerier;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class JobServiceForWeb {

    private final TaskMapper taskMapper;
    private final JobConverter jobConvertor;
    private final HotJobHolder hotJobHolder;
    private final JobLoader jobLoader;
    private final SwTaskScheduler swTaskScheduler;
    private final ResultQuerier resultQuerier;
    private final UserService userService;
    private final ProjectService projectService;
    private final JobDao jobDao;
    private final JobUpdateHelper jobUpdateHelper;

    private final TrashService trashService;

    private final JobCreator jobCreator;

    public JobServiceForWeb(
            TaskMapper taskMapper, JobConverter jobConvertor,
            HotJobHolder hotJobHolder,
            ProjectService projectService, JobDao jobDao, JobLoader jobLoader,
            ResultQuerier resultQuerier,
            UserService userService, JobUpdateHelper jobUpdateHelper, TrashService trashService,
            SwTaskScheduler swTaskScheduler, JobCreator jobCreator) {
        this.taskMapper = taskMapper;
        this.jobConvertor = jobConvertor;
        this.hotJobHolder = hotJobHolder;
        this.projectService = projectService;
        this.jobDao = jobDao;
        this.jobLoader = jobLoader;
        this.resultQuerier = resultQuerier;
        this.userService = userService;
        this.jobUpdateHelper = jobUpdateHelper;
        this.trashService = trashService;
        this.swTaskScheduler = swTaskScheduler;
        this.jobCreator = jobCreator;
    }

    public PageInfo<JobVo> listJobs(String projectUrl, Long modelId, PageParams pageParams) {
        Long projectId = projectService.getProjectId(projectUrl);
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        var jobEntities = jobDao.listJobs(projectId, modelId);
        return PageUtil.toPageInfo(jobEntities, jobConvertor::convert);
    }

    public JobVo findJob(String projectUrl, String jobUrl) {
        var entity = jobDao.findJobEntity(jobUrl);
        if (entity == null) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, String.format("Unable to find job %s", jobUrl)),
                    HttpStatus.BAD_REQUEST);
        }

        return jobConvertor.convert(entity);
    }

    public Object getJobResult(String projectUrl, String jobUrl) {
        Long jobId = jobDao.getJobId(jobUrl);
        return resultQuerier.resultOfJob(jobId);
    }

    public Boolean updateJobComment(String projectUrl, String jobUrl, String comment) {
        return jobDao.updateJobComment(jobUrl, comment);
    }

    public Boolean updateJobPinStatus(String projectUrl, String jobUrl, Boolean pinned) {
        return jobDao.updateJobPinStatus(jobUrl, pinned);
    }

    public Boolean removeJob(String projectUrl, String jobUrl) {
        var job = jobDao.findJob(jobUrl);
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
    public Long createJob(String projectUrl,
            String modelVersionUrl, String datasetVersionUrls, String runtimeVersionUrl,
            String comment, String resourcePool,
            String handler, String stepSpecOverWrites, JobType type,
            DevWay devWay, boolean devMode, String devPassword, Long ttlInSec) {
        User user = userService.currentUserDetail();
        var project = projectService.findProject(projectUrl);
        return jobCreator.createJob(project, modelVersionUrl, datasetVersionUrls, runtimeVersionUrl, comment,
                resourcePool, handler, stepSpecOverWrites, type, devWay, devMode, devPassword, ttlInSec, user).getId();
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
     * transactional jobStatus->TO_CANCEL; RUNNING/PREPARING/ASSIGNING->TO_CANCEL;CREATED/PAUSED/UNKNOWN->CANCELED
     */
    @Transactional
    public void cancelJob(String jobUrl) {
        Long jobId = jobDao.getJobId(jobUrl);
        Collection<Job> jobs = hotJobHolder.ofIds(List.of(jobId));
        if (null == jobs || jobs.isEmpty()) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "freeze job can't be canceled "),
                    HttpStatus.BAD_REQUEST);
        }
        Job job = jobs.stream().findAny().get();
        if (job.getStatus() != JobStatus.RUNNING
                && job.getStatus() != JobStatus.PAUSED) {
            throw new SwValidationException(ValidSubject.JOB, "not RUNNING/PAUSED job can't be canceled ");
        }

        List<Task> directlyCanceledTasks = tasksOfJob(job)
                .filter(task -> task.getStatus() != TaskStatus.SUCCESS
                        && task.getStatus() != TaskStatus.FAIL
                        && task.getStatus() != TaskStatus.CREATED)
                .collect(Collectors.toList());
        batchPersistTaskStatus(directlyCanceledTasks, TaskStatus.CANCELLING);
        updateWithoutPersistWatcher(directlyCanceledTasks, TaskStatus.CANCELLING);
    }

    public List<Job> listHotJobs() {
        return jobDao.findJobByStatusIn(new ArrayList<>(JobStatusMachine.HOT_JOB_STATUS));
    }

    private Stream<Task> tasksOfJob(Job job) {
        return job.getSteps().stream()
                .map(Step::getTasks)
                .flatMap(Collection::stream);
    }

    /**
     * transactional jobStatus RUNNING->PAUSED; taskStatus CREATED->PAUSED
     */
    @Transactional
    public void pauseJob(String jobUrl) {
        Long jobId = jobDao.getJobId(jobUrl);
        Collection<Job> jobs = hotJobHolder.ofIds(List.of(jobId));
        if (null == jobs || jobs.isEmpty()) {
            throw new SwValidationException(ValidSubject.JOB, "frozen job can't be paused ");
        }
        Job job = jobs.stream().findAny().get();
        List<Task> notRunningTasks = tasksOfJob(job)
                .filter(task -> task.getStatus() != TaskStatus.SUCCESS
                        && task.getStatus() != TaskStatus.FAIL
                        && task.getStatus() != TaskStatus.CREATED)
                .collect(Collectors.toList());
        if (notRunningTasks.isEmpty()) {
            return;
        }
        batchPersistTaskStatus(notRunningTasks, TaskStatus.PAUSED);
        updateWithoutPersistWatcher(notRunningTasks, TaskStatus.PAUSED);

    }

    private void updateWithoutPersistWatcher(List<Task> tasks, TaskStatus taskStatus) {
        CompletableFuture.runAsync(() -> {
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(Set.of(TaskWatcherForPersist.class));
            tasks.forEach(task -> task.updateStatus(taskStatus));
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
        });
    }

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 1000;

    private void batchPersistTaskStatus(Collection<Task> tasks, TaskStatus taskStatus) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        //save to db
        List<Long> taskIds = tasks.parallelStream().map(Task::getId).collect(
                Collectors.toList());
        BatchOperateHelper.doBatch(
                taskIds,
                taskStatus,
                (tsks, status) -> taskMapper.updateTaskStatus(new ArrayList<>(tsks), status),
                MAX_BATCH_SIZE);
    }

    /**
     * jobStatus PAUSED->RUNNING; taskStatus PAUSED->CREATED jobStatus FAILED->RUNNING; taskStatus PAUSED->CREATED
     */
    public void resumeJob(String jobUrl) {
        Long jobId = jobDao.getJobId(jobUrl);
        Job job = jobDao.findJobById(jobId);
        if (null == job) {
            throw new SwValidationException(ValidSubject.JOB, "job not exists");
        }
        if (job.getStatus() != JobStatus.PAUSED
                && job.getStatus() != JobStatus.FAIL
                && job.getStatus() != JobStatus.CANCELED) {
            throw new SwValidationException(ValidSubject.JOB, "only failed/paused/canceled job can be resumed ");
        }
        job = jobLoader.load(job, true);
        jobUpdateHelper.updateJob(job);
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
            var resp = swTaskScheduler.exec(task, execRequest.getCommand()).get();
            return ExecResponse.builder().stdout(resp[0]).stderr(resp[1]).build();
        } catch (InterruptedException | ExecutionException e) {
            throw new SwProcessException(SwProcessException.ErrorType.K8S, "exec task failed", e);
        }
    }
}
