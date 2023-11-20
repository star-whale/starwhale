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
import ai.starwhale.mlops.api.protocol.event.Event.EventResourceType;
import ai.starwhale.mlops.api.protocol.event.EventRequest;
import ai.starwhale.mlops.api.protocol.event.EventVo;
import ai.starwhale.mlops.api.protocol.job.ExecRequest;
import ai.starwhale.mlops.api.protocol.job.ExecResponse;
import ai.starwhale.mlops.api.protocol.job.JobModifyPinRequest;
import ai.starwhale.mlops.api.protocol.job.JobModifyRequest;
import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.api.protocol.job.ModelServingRequest;
import ai.starwhale.mlops.api.protocol.job.ModelServingStatusVo;
import ai.starwhale.mlops.api.protocol.job.ModelServingVo;
import ai.starwhale.mlops.api.protocol.job.RuntimeSuggestionVo;
import ai.starwhale.mlops.api.protocol.run.RunVo;
import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.InvokerManager;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.configuration.FeaturesProperties;
import ai.starwhale.mlops.domain.dag.DagQuerier;
import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.event.EventService;
import ai.starwhale.mlops.domain.ft.FineTuneAppService;
import ai.starwhale.mlops.domain.job.BizType;
import ai.starwhale.mlops.domain.job.JobServiceForWeb;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.ModelServingService;
import ai.starwhale.mlops.domain.job.RuntimeSuggestionService;
import ai.starwhale.mlops.domain.job.converter.UserJobConverter;
import ai.starwhale.mlops.domain.run.RunService;
import ai.starwhale.mlops.domain.task.TaskService;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Tag(name = "Job")
@RequestMapping("${sw.controller.api-prefix}")
public class JobController {

    private final JobServiceForWeb jobServiceForWeb;
    private final FineTuneAppService fineTuneAppService;
    private final TaskService taskService;
    private final ModelServingService modelServingService;
    private final RuntimeSuggestionService runtimeSuggestionService;
    private final IdConverter idConvertor;
    private final DagQuerier dagQuerier;
    private final InvokerManager<String, String> jobActions;
    private final FeaturesProperties featuresProperties;
    private final EventService eventService;

    private final RunService runService;
    private final UserJobConverter userJobConverter;

    public JobController(
            JobServiceForWeb jobServiceForWeb,
            FineTuneAppService fineTuneAppService, TaskService taskService,
            ModelServingService modelServingService,
            RuntimeSuggestionService runtimeSuggestionService,
            IdConverter idConvertor,
            DagQuerier dagQuerier,
            FeaturesProperties featuresProperties,
            EventService eventService,
            RunService runService,
            UserJobConverter userJobConverter
    ) {
        this.jobServiceForWeb = jobServiceForWeb;
        this.fineTuneAppService = fineTuneAppService;
        this.taskService = taskService;
        this.modelServingService = modelServingService;
        this.runtimeSuggestionService = runtimeSuggestionService;
        this.idConvertor = idConvertor;
        this.dagQuerier = dagQuerier;
        this.featuresProperties = featuresProperties;
        this.eventService = eventService;
        this.runService = runService;
        this.userJobConverter = userJobConverter;

        var actions = InvokerManager.<String, String>create()
                .addInvoker("cancel", jobServiceForWeb::cancelJob);
        if (featuresProperties.isJobPauseEnabled()) {
            actions.addInvoker("pause", jobServiceForWeb::pauseJob);
        }
        if (featuresProperties.isJobResumeEnabled()) {
            actions.addInvoker("resume", jobServiceForWeb::resumeJob);
        }
        this.jobActions = actions.unmodifiable();
    }

    @Operation(summary = "Get the list of jobs")
    @GetMapping(value = "/project/{projectUrl}/job", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<PageInfo<JobVo>>> listJobs(
            @PathVariable String projectUrl,
            @RequestParam(required = false) String swmpId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        PageInfo<JobVo> jobVos = jobServiceForWeb.listJobs(projectUrl, idConvertor.revert(swmpId),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(jobVos));
    }

    @Operation(summary = "Job information")
    @GetMapping(value = "/project/{projectUrl}/job/{jobUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<JobVo>> getJob(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl
    ) {
        JobVo job = jobServiceForWeb.findJob(projectUrl, jobUrl);
        return ResponseEntity.ok(Code.success.asResponse(job));
    }

    @Operation(summary = "Get the list of tasks")
    @GetMapping(value = "/project/{projectUrl}/job/{jobUrl}/task", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<PageInfo<TaskVo>>> listTasks(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {

        PageInfo<TaskVo> pageInfo = taskService.listTasks(jobUrl,
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build());
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Operation(summary = "Get task info")
    @GetMapping(value = "/project/{projectUrl}/job/{jobUrl}/task/{taskUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<TaskVo>> getTask(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl,
            @PathVariable String taskUrl
    ) {
        return ResponseEntity.ok(Code.success.asResponse(taskService.getTask(taskUrl)));
    }

    @Operation(summary = "Get runs info")
    @GetMapping(value = "/project/{projectUrl}/job/{jobUrl}/task/{taskId}/run",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<List<RunVo>>> getRuns(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl,
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(Code.success.asResponse(runService.runOfTask(taskId)));
    }

    @Operation(summary = "Create a new job")
    @PostMapping(value = "/project/{projectUrl}/job", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> createJob(
            @PathVariable String projectUrl,
            @Valid @RequestBody JobRequest jobRequest
    ) {
        if (jobRequest.isDevMode() && !featuresProperties.isJobDevEnabled()) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "dev mode is not enabled"),
                    HttpStatus.BAD_REQUEST
            );
        }
        Long jobId = null;
        if (jobRequest.getBizType() == BizType.FINE_TUNE) {
            Long spaceId = idConvertor.revert(jobRequest.getBizId());
            if (jobRequest.getType() == JobType.FINE_TUNE) {
                jobId = fineTuneAppService.createFineTune(projectUrl, spaceId, jobRequest);
            } else if (jobRequest.getType() == JobType.EVALUATION) {
                jobId = fineTuneAppService.createEvaluationJob(projectUrl, spaceId, jobRequest);
            }
        } else {
            jobId = jobServiceForWeb.createJob(userJobConverter.convert(projectUrl, jobRequest));
        }
        if (jobId == null)  {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "job request is invalid"),
                    HttpStatus.BAD_REQUEST
            );
        }
        return ResponseEntity.ok(Code.success.asResponse(idConvertor.convert(jobId)));
    }

    @Operation(summary = "Job Action")
    @PostMapping(value = "/project/{projectUrl}/job/{jobUrl}/{action}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> action(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl,
            @PathVariable String action
    ) {
        try {
            jobActions.invoke(action, jobUrl);
        } catch (UnsupportedOperationException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.JOB, "failed to invoke action", e),
                    HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(Code.success.asResponse("Success: " + action));
    }

    @Operation(summary = "Set Job Comment")
    @PutMapping(value = "/project/{projectUrl}/job/{jobUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> modifyJobComment(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl,
            @Valid @RequestBody JobModifyRequest jobRequest
    ) {
        Boolean res = jobServiceForWeb.updateJobComment(projectUrl, jobUrl, jobRequest.getComment());

        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Update job comment failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Pin Job")
    @PostMapping(value = "/project/{projectUrl}/job/{jobUrl}/pin", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> modifyJobPinStatus(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl,
            @Valid @RequestBody JobModifyPinRequest jobRequest
    ) {
        Boolean res = jobServiceForWeb.updateJobPinStatus(projectUrl, jobUrl, jobRequest.isPinned());

        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Update job pin status failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "DAG of Job")
    @GetMapping(value = "/project/{projectUrl}/job/{jobUrl}/dag", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<Graph>> getJobDag(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl
    ) {
        return ResponseEntity.ok(Code.success.asResponse(dagQuerier.dagOfJob(jobUrl)));
    }

    @Operation(summary = "Remove job")
    @DeleteMapping(value = "/project/{projectUrl}/job/{jobUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> removeJob(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl
    ) {
        Boolean res = jobServiceForWeb.removeJob(projectUrl, jobUrl);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Remove job failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Recover job")
    @PostMapping(value = "/project/{projectUrl}/job/{jobUrl}/recover", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    public ResponseEntity<ResponseMessage<String>> recoverJob(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl
    ) {
        Boolean res = jobServiceForWeb.recoverJob(projectUrl, jobUrl);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Recover job failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Create a new model serving job")
    @PostMapping(value = "/project/{projectUrl}/serving", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<ModelServingVo>> createModelServing(
            @PathVariable String projectUrl,
            @Valid @RequestBody ModelServingRequest request
    ) {
        if (!featuresProperties.isOnlineEvalEnabled()) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "Online evaluation is not enabled."),
                    HttpStatus.BAD_REQUEST);
        }
        var resp = modelServingService.create(
                projectUrl,
                request.getModelVersionUrl(),
                request.getRuntimeVersionUrl(),
                request.getResourcePool(),
                request.getSpec()
        );

        return ResponseEntity.ok(Code.success.asResponse(resp));
    }

    @Operation(summary = "Get the events of the model serving job")
    @GetMapping(value = "/project/{projectId}/serving/{servingId}/status",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<ModelServingStatusVo>> getModelServingStatus(
            @PathVariable Long projectId,
            @PathVariable Long servingId
    ) {
        if (!featuresProperties.isOnlineEvalEnabled()) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.JOB, "Online evaluation is not enabled."),
                    HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(Code.success.asResponse(modelServingService.getStatus(servingId)));
    }

    @Operation(summary = "Get suggest runtime for eval or online eval")
    @GetMapping(value = "/job/suggestion/runtime", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<RuntimeSuggestionVo>> getRuntimeSuggestion(
            // projectId may be unnecessary in the future, so we do not put this in the uri parts
            @Valid @RequestParam Long projectId,
            @Valid @RequestParam(required = false) Long modelVersionId
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                RuntimeSuggestionVo.builder()
                        .runtimes(runtimeSuggestionService.getSuggestions(projectId, modelVersionId))
                        .build())
        );
    }

    @Operation(summary = "Execute command in running task")
    @PostMapping(value = "/project/{projectUrl}/job/{jobUrl}/task/{taskId}/exec",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<ExecResponse>> exec(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl,
            @PathVariable String taskId,
            @Valid @RequestBody ExecRequest execRequest
    ) {
        var resp = jobServiceForWeb.exec(projectUrl, jobUrl, taskId, execRequest);
        return ResponseEntity.ok(Code.success.asResponse(resp));
    }

    @Operation(summary = "Add event to job or task")
    @PostMapping(value = "/project/{projectUrl}/job/{jobUrl}/event", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> addEvent(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl,
            @Valid @RequestBody EventRequest request
    ) {
        eventService.addEventForJob(jobUrl, request);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Get events of job or task")
    @GetMapping(value = "/project/{projectUrl}/job/{jobUrl}/event", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<List<EventVo>>> getEvents(
            @PathVariable String projectUrl,
            @PathVariable String jobUrl,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long runId
    ) {
        EventRequest.RelatedResource request = null;
        if (runId != null) {
            request = new EventRequest.RelatedResource();
            request.setEventResourceType(EventResourceType.RUN);
            request.setId(runId);
        } else if (taskId != null) {
            request = new EventRequest.RelatedResource();
            request.setEventResourceType(EventResourceType.TASK);
            request.setId(taskId);
        }

        return ResponseEntity.ok(Code.success.asResponse(eventService.getEventsForJob(jobUrl, request)));
    }
}
