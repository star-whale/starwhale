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
import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.api.protocol.task.TaskVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.InvokerManager;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.dag.DAGQuerier;
import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.job.JobService;
import ai.starwhale.mlops.domain.task.TaskService;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class JobController implements JobApi{

    @Resource
    private JobService jobService;

    @Resource
    private TaskService taskService;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private DAGQuerier dagQuerier;


    private final InvokerManager<String, String> JOB_ACTIONS = InvokerManager.<String, String>create()
        .addInvoker("cancel", (String jobUrl) -> jobService.cancelJob(jobUrl))
        .addInvoker("pause", (String jobUrl) -> jobService.pauseJob(jobUrl))
        .addInvoker("resume", (String jobUrl) -> jobService.resumeJob(jobUrl))
        .unmodifiable();

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<JobVO>>> listJobs(String projectUrl, String swmpId,
        Integer pageNum, Integer pageSize) {

        PageInfo<JobVO> jobVOS = jobService.listJobs(projectUrl, idConvertor.revert(swmpId),
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(jobVOS));
    }

    @Override
    public ResponseEntity<ResponseMessage<JobVO>> findJob(String projectUrl, String jobUrl) {
        JobVO job = jobService.findJob(projectUrl, jobUrl);
        return ResponseEntity.ok(Code.success.asResponse(job));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<TaskVO>>> listTasks(String projectUrl,
        String jobUrl, Integer pageNum, Integer pageSize) {

        PageInfo<TaskVO> pageInfo = taskService.listTasks(jobUrl,
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
            jobRequest.getDevice(),
            jobRequest.getDeviceAmount(),
            jobRequest.getComment(),
            jobRequest.getResourcePool());

        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(jobId)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> action(String projectUrl, String jobUrl,
        String action) {
        try {
            JOB_ACTIONS.invoke(action, jobUrl);
        } catch (UnsupportedOperationException e) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB)
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

        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Update job comment failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<Graph>> getJobDAG(String projectUrl, String jobUrl) {
        return ResponseEntity.ok(Code.success.asResponse(dagQuerier.dagOfJob(jobUrl,true)));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> removeJob(String projectUrl, String jobUrl) {
        Boolean res = jobService.removeJob(projectUrl, jobUrl);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Remove job failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverJob(String projectUrl, String jobUrl) {
        Boolean res = jobService.recoverJob(projectUrl, jobUrl);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Recover job failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }
}
