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

package ai.starwhale.mlops.resulting;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.dag.DAGEditor;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.bo.JobBoConverter;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.task.cache.LivingTaskCache;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.ResultPathConverter;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskBoConverter;
import ai.starwhale.mlops.domain.task.bo.cmp.CMPRequest;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

    final LivingTaskCache livingTaskCache;

    final StorageAccessService storageAccessService;

    final TaskBoConverter taskBoConverter;

    final SWTaskScheduler swTaskScheduler;

    final ResultPathConverter resultPathConverter;

    final DAGEditor dagEditor;

    public CMPTaskFire(JobBoConverter jobBoConverter,
        TaskMapper taskMapper,
        JobMapper jobMapper,
        @Qualifier("cacheWrapperForWatch") LivingTaskCache livingTaskCache,
        StorageAccessService storageAccessService,
        TaskBoConverter taskBoConverter,
        SWTaskScheduler swTaskScheduler,
        ResultPathConverter resultPathConverter, DAGEditor dagEditor) {
        this.jobBoConverter = jobBoConverter;
        this.taskMapper = taskMapper;
        this.jobMapper = jobMapper;
        this.livingTaskCache = livingTaskCache;
        this.storageAccessService = storageAccessService;
        this.taskBoConverter = taskBoConverter;
        this.swTaskScheduler = swTaskScheduler;
        this.resultPathConverter = resultPathConverter;
        this.dagEditor = dagEditor;
    }

    @Transactional
    @Scheduled(initialDelay = 10000,fixedDelay = 1000*10)
    public void onJobCollect(){
        jobMapper.findJobByStatusIn(List.of(JobStatus.TO_COLLECT_RESULT))
            .parallelStream()
            .forEach(jobEntity -> dispatchCMPTask(jobBoConverter.fromEntity(jobEntity)));
    }

    private void dispatchCMPTask(Job job) {
        Collection<Task> tasks = livingTaskCache.ofJob(job.getId());
        List<String> allPPLTaskResults = tasks.parallelStream().flatMap(task -> {
                try {
                    return storageAccessService.list(task.getResultRootPath().resultDir());
                } catch (IOException e) {
                    throw new SWProcessException(ErrorType.STORAGE).tip("list task result dir failed");
                }
            })
            .collect(Collectors.toList());

        String resultPathStr = null;
        try {
            ResultPath taskResultPath = new ResultPath(job.getResultDir());
            resultPathStr = resultPathConverter.toString(taskResultPath);
            jobMapper.updateJobResultPath(job.getId(),taskResultPath.resultDir());
        } catch (JsonProcessingException e) {
            log.error("convert job result path failed {}",job.getId(),e);
            throw new SWValidationException(ValidSubject.TASK).tip("convert result path failed");
        }

        TaskEntity taskEntity = TaskEntity.builder()
            .jobId(job.getId())
            .taskRequest(new CMPRequest(allPPLTaskResults).toString())
            .taskType(TaskType.CMP)
            .resultPath(resultPathStr)
            .taskStatus(TaskStatus.CREATED)
            .taskUuid(UUID.randomUUID().toString())
            .build();
        taskMapper.addTask(taskEntity);
        jobMapper.updateJobStatus(List.of(job.getId()),JobStatus.COLLECTING_RESULT);

        List<Task> cmpTasks = taskBoConverter.fromTaskEntity(List.of(taskEntity), job);
        cmpTasks.parallelStream().forEach(t->{
            dagEditor.taskStatusChange(t,TaskStatus.CREATED);
        });
        livingTaskCache.adopt(cmpTasks,TaskStatus.CREATED);
        swTaskScheduler.adoptTasks(livingTaskCache.ofIds(cmpTasks.parallelStream().map(Task::getId).collect(Collectors.toList())), Clazz.CPU);
    }

}
