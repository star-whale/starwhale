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
import ai.starwhale.mlops.api.protocol.evaluation.AttributeVo;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigVo;
import ai.starwhale.mlops.api.protocol.evaluation.SummaryVo;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.evaluation.EvaluationService;
import ai.starwhale.mlops.domain.evaluation.bo.ConfigQuery;
import ai.starwhale.mlops.domain.evaluation.bo.SummaryFilter;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Tag(name = "Evaluation")
@Validated
@RequestMapping("${sw.controller.api-prefix}")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Operation(summary = "List Evaluation Summary Attributes")
    @GetMapping(value = "/project/{projectUrl}/evaluation/view/attribute",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<AttributeVo>>> listAttributes(
            @PathVariable String projectUrl
    ) {
        List<AttributeVo> vos = evaluationService.listAttributeVo();
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }

    @Operation(summary = "Get View Config")
    @GetMapping(
            value = "/project/{projectUrl}/evaluation/view/config",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<ConfigVo>> getViewConfig(
            @PathVariable String projectUrl,
            @RequestParam String name
    ) {
        ConfigVo viewConfig = evaluationService.getViewConfig(ConfigQuery.builder()
                .projectUrl(projectUrl)
                .name(name)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(viewConfig));
    }

    @Operation(summary = "Create or Update View Config")
    @PostMapping(
            value = "/project/{projectUrl}/evaluation/view/config",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> createViewConfig(
            @PathVariable String projectUrl,
            @RequestBody ConfigRequest configRequest
    ) {
        Boolean res = evaluationService.createViewConfig(projectUrl, configRequest);
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Create view config failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "List Evaluation Summary")
    @GetMapping(
            value = "/project/{projectUrl}/evaluation",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<SummaryVo>>> listEvaluationSummary(
            @PathVariable String projectUrl,
            @RequestParam String filter,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        PageInfo<SummaryVo> vos = evaluationService.listEvaluationSummary(
                projectUrl,
                SummaryFilter.parse(filter),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build()
        );
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }
}
