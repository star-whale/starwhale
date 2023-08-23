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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Log")
@Validated
public interface LogApi {

    @Operation(summary = "list the log files of a task")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/log/offline/{taskId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ResponseMessage<List<String>>> offlineLogs(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "id of a task",
                    schema = @Schema())
            @PathVariable("taskId")
            Long taskId);

    @Operation(summary = "Get the list of device types")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/log/offline/{taskId}/{fileName}",
            produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> logContent(
            @Parameter(
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
