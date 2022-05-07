package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.job.JobRequest;
import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.api.protocol.task.TaskVO;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Job")
@Validated
public interface JobApi {




    @Operation(summary = "Get the list of jobs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "ok",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageInfo.class)))})
    @GetMapping(value = "/project/{projectId}/job")
    ResponseEntity<ResponseMessage<PageInfo<JobVO>>> listJobs(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Valid @RequestParam(value = "swmpId", required = false) String swmpId,
        @Valid @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
        @Valid @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize);


    @Operation(summary = "Job information")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "OK",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = JobVO.class)))
        })
    @GetMapping(value = "/project/{projectId}/job/{jobId}")
    ResponseEntity<ResponseMessage<JobVO>> findJob( @Parameter(
        in = ParameterIn.PATH,
        description = "Project id",
        schema = @Schema())
    @PathVariable("projectId")
        String projectId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("jobId")
            String jobId);


    @Operation(summary = "Get the list of tasks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "ok",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageInfo.class)))})
    @GetMapping(value = "/project/{projectId}/job/{jobId}/task")
    ResponseEntity<ResponseMessage<PageInfo<TaskVO>>> listTasks(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Job id",
            schema = @Schema())
        @PathVariable("jobId")
            String jobId,
        @Valid @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
        @Valid @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize);

    @Operation(summary = "Create a new job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/project/{projectId}/job")
    ResponseEntity<ResponseMessage<String>> createJob(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Valid @RequestBody JobRequest jobRequest);

    @Operation(summary = "Job Action")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/project/{projectId}/job/{jobId}/{action}")
    ResponseEntity<ResponseMessage<String>> action(
        @Parameter(
            in = ParameterIn.PATH,
            description = "Project id",
            schema = @Schema())
        @PathVariable("projectId")
            String projectId,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Job id",
            schema = @Schema())
        @PathVariable("jobId")
            String jobId,
        @Parameter(
            in = ParameterIn.PATH,
            description = "Job action",
            schema = @Schema())
        @PathVariable("action")
            String action);

    @Operation(summary = "Job Evaluation Result")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "OK",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Object.class)))
        })
    @GetMapping(value = "/project/{projectId}/job/{jobId}/result")
    ResponseEntity<ResponseMessage<Object>> getJobResult(@Parameter(
        in = ParameterIn.PATH,
        description = "Project id",
        schema = @Schema())
    @PathVariable("projectId")
        String projectId,
        @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
        @PathVariable("jobId")
            String jobId);
}
