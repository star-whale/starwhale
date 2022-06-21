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
import ai.starwhale.mlops.api.protocol.evaluation.AttributeVO;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigVO;
import ai.starwhale.mlops.api.protocol.evaluation.SummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Evaluation")
@Validated
public interface EvaluationApi {

    @Operation(summary = "List Evaluation Summary Attributes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(value = "/evaluation/view/attribute")
    ResponseEntity<ResponseMessage<List<AttributeVO>>> listAttributes();


    @Operation(summary = "Get View Config")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(value = "/evaluation/view/config")
    ResponseEntity<ResponseMessage<ConfigVO>> getViewConfig(
        @Valid @RequestParam(value = "projectUrl") String projectUrl,
        @Valid @RequestParam(value = "name") String name
    );

    @Operation(summary = "Create or Update View Config")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(value = "/evaluation/view/config")
    ResponseEntity<ResponseMessage<String>> createViewConfig(
        @Valid @RequestBody ConfigRequest configRequest
    );

    @Operation(summary = "List Evaluation Summary")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(value = "/evaluation")
    ResponseEntity<ResponseMessage<List<SummaryVO>>> listEvaluationSummary(
        @Valid @RequestParam(value = "filter") String filter
    );
}
