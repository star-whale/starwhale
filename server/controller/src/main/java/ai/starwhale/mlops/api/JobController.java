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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.job.JobModifyRequest;
import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.InvokerManager;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.dag.DagQuerier;
import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.job.JobService;
import ai.starwhale.mlops.domain.task.TaskService;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class JobController implements JobApi {

    private final JobService jobService;
    private final TaskService taskService;
    private final IdConvertor idConvertor;
    private final DagQuerier dagQuerier;
    private final InvokerManager<String, String> jobActions;

    public JobController(JobService jobService, TaskService taskService, IdConvertor idConvertor,
            DagQuerier dagQuerier) {
        this.jobService = jobService;
        this.taskService = taskService;
        this.idConvertor = idConvertor;
        this.dagQuerier = dagQuerier;
        this.jobActions = InvokerManager.<String, String>create()
                .addInvoker("cancel", jobService::cancelJob)
                .addInvoker("pause", jobService::pauseJob)
                .addInvoker("resume", jobService::resumeJob)
                .unmodifiable();
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<JobVo>>> listJobs(String projectUrl, String modelId,
            Integer pageNum, Integer pageSize) {

        PageInfo<JobVo> jobVos = jobService.listJobs(projectUrl, idConvertor.revert(modelId),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(jobVos));
    }

    @Override
    public ResponseEntity<ResponseMessage<JobVo>> findJob(String projectUrl, String jobUrl) {
        JobVo job = jobService.findJob(projectUrl, jobUrl);
        return ResponseEntity.ok(Code.success.asResponse(job));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<TaskVo>>> listTasks(String projectUrl,
            String jobUrl, Integer pageNum, Integer pageSize) {

        PageInfo<TaskVo> pageInfo = taskService.listTasks(jobUrl,
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createJob(String projectUrl,
            JobRequest jobRequest) {
        Long jobId = jobService.createJob(projectUrl,
                jobRequest.getModelVersionUrl(),
                jobRequest.getDatasetVersionUrls(),
                jobRequest.getRuntimeVersionUrl(),
                jobRequest.getComment(),
                jobRequest.getResourcePool(),
                jobRequest.getStepSpecOverWrites());

        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(jobId)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> action(String projectUrl, String jobUrl,
            String action) {
        try {
            jobActions.invoke(action, jobUrl);
        } catch (UnsupportedOperationException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.JOB)
                    .tip(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(Code.success.asResponse("Success: " + action));
    }

    @Override
    public ResponseEntity<ResponseMessage<Object>> getJobResult(String projectUrl, String jobUrl) {
        Object jobResult = jobService.getJobResult(projectUrl, jobUrl);
        return ResponseEntity.ok(Code.success.asResponse(jobResult));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> modifyJobComment(String projectUrl, String jobUrl,
            JobModifyRequest jobModifyRequest) {
        Boolean res = jobService.updateJobComment(projectUrl, jobUrl, jobModifyRequest.getComment());

        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Update job comment failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<Graph>> getJobDag(String projectUrl, String jobUrl) {
        return ResponseEntity.ok(Code.success.asResponse(dagQuerier.dagOfJob(jobUrl)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> removeJob(String projectUrl, String jobUrl) {
        Boolean res = jobService.removeJob(projectUrl, jobUrl);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Remove job failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverJob(String projectUrl, String jobUrl) {
        Boolean res = jobService.recoverJob(projectUrl, jobUrl);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB).tip("Recover job failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }
}
