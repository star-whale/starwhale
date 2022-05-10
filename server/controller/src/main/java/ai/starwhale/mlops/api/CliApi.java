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
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "CLI")
@Validated
public interface CliApi {

    @Operation(summary = "Get the list of models -- for cli")
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
    @GetMapping(value = "/cli/model")
    ResponseEntity<ResponseMessage<List<SWModelPackageInfoVO>>> listModelInfo(
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Owner name prefix of the models. Use @self to get the current user's own models",
            schema = @Schema())
        @RequestParam(value = "ownerName", required = false)
        String ownerName,
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Project Name prefix of the models",
            schema = @Schema())
        @RequestParam(value = "projectName", required = false)
        String projectName,
        @Parameter(
            in = ParameterIn.QUERY,
            description = "Model name prefix to search for",
            schema = @Schema())
        @Valid
        @RequestParam(value = "modelName", required = false)
        String modelName);

}
