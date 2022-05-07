/**
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
import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.api.protocol.task.TaskVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.InvokerManager;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.job.JobService;
import ai.starwhale.mlops.domain.task.TaskService;
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


    private final InvokerManager<String, Long> JOB_ACTIONS = InvokerManager.<String, Long>create()
        .addInvoker("cancel", (Long id) -> jobService.cancelJob(id))
        .addInvoker("pause", (Long id) -> jobService.pauseJob(id))
        .addInvoker("resume", (Long id) -> jobService.resumeJob(id))
        .unmodifiable();

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<JobVO>>> listJobs(String projectId, String swmpId,
        Integer pageNum, Integer pageSize) {

        PageInfo<JobVO> jobVOS = jobService.listJobs(projectId, swmpId, new PageParams(pageNum, pageSize));
        return ResponseEntity.ok(Code.success.asResponse(jobVOS));
    }

    @Override
    public ResponseEntity<ResponseMessage<JobVO>> findJob(String projectId, String jobId) {
        JobVO job = jobService.findJob(projectId, jobId);
        return ResponseEntity.ok(Code.success.asResponse(job));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<TaskVO>>> listTasks(String projectId,
        String jobId, Integer pageNum, Integer pageSize) {

        PageInfo<TaskVO> pageInfo = taskService.listTasks(jobId, new PageParams(pageNum, pageSize));
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createJob(String projectId,
        JobRequest jobRequest) {
        String id = jobService.createJob(jobRequest, projectId);

        return ResponseEntity.ok(Code.success.asResponse(id));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> action(String projectId, String jobId,
        String action) {
        Long iJobId = idConvertor.revert(jobId);
        try {
            JOB_ACTIONS.invoke(action, iJobId);
        } catch (UnsupportedOperationException e) {
            throw new StarWhaleApiException(new SWValidationException(ValidSubject.JOB)
                .tip(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(Code.success.asResponse("Success: " + action));
    }

    @Override
    public ResponseEntity<ResponseMessage<Object>> getJobResult(String projectId,
        String jobId) {
        Object jobResult = jobService.getJobResult(projectId, jobId);
        return ResponseEntity.ok(Code.success.asResponse(jobResult));
    }

}
