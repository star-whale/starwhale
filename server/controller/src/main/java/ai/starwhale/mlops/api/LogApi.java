/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Log")
@Validated
public interface LogApi {

    @Operation(summary = "list the log files of a task")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = List.class)))
        })
    @GetMapping(value = "/log/offline/{taskId}")
    ResponseEntity<ResponseMessage<List<String>>> offlineLogs(
        @Parameter(
            in = ParameterIn.PATH,
            description = "id of a task",
            schema = @Schema())
        @PathVariable("taskId")
            Long taskId);

    @Operation(summary = "Get the list of device types")
    @ApiResponses(
        value = {
            @ApiResponse(
                responseCode = "200",
                description = "ok",
                content =
                @Content(
                    mediaType = "plain/text",
                    schema = @Schema(implementation = String.class)))
        })
    @GetMapping(value = "/log/offline/{taskId}/{fileName}")
    ResponseEntity<String> logContent(@Parameter(
        in = ParameterIn.PATH,
        description = "id of a task",
        schema = @Schema())
    @PathVariable("taskId")
        Long taskId,
        @Parameter(
            in = ParameterIn.PATH,
            description = "the name of the file",
            schema = @Schema())
        @PathVariable("fileName")
            String fileName);
}
