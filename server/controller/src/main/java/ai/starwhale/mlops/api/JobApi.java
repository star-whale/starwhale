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

import ai.starwhale.mlops.api.protocol.ResponseMessage;
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
import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.domain.dag.bo.Graph;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Job")
@Validated
@ConditionalOnProperty(name = "sw.modules.jobs.enabled", havingValue = "true")
public interface JobApi {


    @Operation(summary = "Get the list of jobs")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/job",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<JobVo>>> listJobs(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Valid @RequestParam(value = "swmpId", required = false) String swmpId,
            @Valid @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @Valid @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize);


    @Operation(summary = "Job information")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/job/{jobUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<JobVo>> findJob(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("jobUrl")
                    String jobUrl);


    @Operation(summary = "Get the list of tasks")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/job/{jobUrl}/task",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<TaskVo>>> listTasks(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Job Url",
                    schema = @Schema())
            @PathVariable("jobUrl")
                    String jobUrl,
            @Valid @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @Valid @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize);

    @Operation(summary = "Get task info")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/job/{jobUrl}/task/{taskUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<TaskVo>> getTask(
            @Parameter(in = ParameterIn.PATH, description = "Project Url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, description = "Job Url", schema = @Schema())
            @PathVariable("jobUrl") String jobUrl,
            @Parameter(in = ParameterIn.PATH, description = "Task Url", schema = @Schema())
            @PathVariable("taskUrl") String taskUrl);

    @Operation(summary = "Create a new job")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/job",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> createJob(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Valid @RequestBody JobRequest jobRequest);

    @Operation(summary = "Job Action")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/job/{jobUrl}/{action}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> action(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Job Url",
                    schema = @Schema())
            @PathVariable("jobUrl")
                    String jobUrl,
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Job action",
                    schema = @Schema())
            @PathVariable("action")
                    String action);

    @Operation(summary = "Job Evaluation Result")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/job/{jobUrl}/result",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Object>> getJobResult(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("jobUrl")
                    String jobUrl);

    @Operation(summary = "Set Job Comment")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/project/{projectUrl}/job/{jobUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> modifyJobComment(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Job id or uuid",
                    required = true,
                    schema = @Schema())
            @PathVariable("jobUrl")
                    String jobUrl,
                    @Valid @RequestBody JobModifyRequest jobRequest);
            
    @Operation(summary = "Pin Job")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "ok") })
    @PostMapping(value = "/project/{projectUrl}/job/{jobUrl}/pin", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> modifyJobPinStatus(
            @Parameter(in = ParameterIn.PATH, description = "Project url", schema = @Schema()) 
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, description = "Job id or uuid", required = true, schema = @Schema())
            @PathVariable("jobUrl") String jobUrl, 
            @Valid @RequestBody JobModifyPinRequest jobRequest);

    @Operation(summary = "DAG of Job")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/job/{jobUrl}/dag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Graph>> getJobDag(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("jobUrl")
                    String jobUrl);

    @Operation(summary = "Remove job")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(
            value = "/project/{projectUrl}/job/{jobUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> removeJob(
            @Valid @PathVariable("projectUrl") String projectUrl,
            @Valid @PathVariable("jobUrl") String jobUrl);

    @Operation(summary = "Recover job")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/job/{jobUrl}/recover",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> recoverJob(
            @Valid @PathVariable("projectUrl") String projectUrl,
            @Valid @PathVariable("jobUrl") String jobUrl);

    @Operation(summary = "Create a new model serving job")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/serving",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<ModelServingVo>> createModelServing(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    schema = @Schema())
            @PathVariable("projectUrl")
                    String projectUrl,
            @Valid @RequestBody ModelServingRequest request);

    @Operation(summary = "Get the events of the model serving job")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectId}/serving/{servingId}/status",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<ModelServingStatusVo>> getModelServingStatus(
            @Parameter(in = ParameterIn.PATH, description = "Project Id")
            @PathVariable("projectId")
            Long projectId,
            @Parameter(in = ParameterIn.PATH, description = "Model Serving Id")
            @PathVariable("servingId")
            Long servingId
    );

    @Operation(summary = "Get suggest runtime for eval or online eval")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/job/suggestion/runtime",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<RuntimeSuggestionVo>> getRuntimeSuggestion(
            // projectId may be unnecessary in the future, so we do not put this in the uri parts
            @Valid @RequestParam(value = "projectId") Long projectId,
            @Valid @RequestParam(value = "modelVersionId", required = false) Long modelVersionId
    );

    @Operation(summary = "Execute command in running task")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/job/{jobUrl}/task/{taskId}/exec",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<ExecResponse>> exec(
            @Parameter(in = ParameterIn.PATH, description = "Project Url", schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,

            @Parameter(in = ParameterIn.PATH, description = "Job Url", schema = @Schema())
            @PathVariable("jobUrl")
            String jobUrl,

            @Parameter(in = ParameterIn.PATH, description = "Task id", schema = @Schema())
            @PathVariable("taskId")
            String taskId,

            @Valid
            @RequestBody
            ExecRequest execRequest
        );
}
