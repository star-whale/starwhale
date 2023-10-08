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
import ai.starwhale.mlops.api.protocol.job.CreateJobTemplateRequest;
import ai.starwhale.mlops.api.protocol.job.JobTemplateVo;
import ai.starwhale.mlops.domain.job.template.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@Validated
@RestController
@Tag(name = "Template")
@RequestMapping("${sw.controller.api-prefix}")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "Add Template for job")
    @PostMapping(value = "/project/{projectUrl}/template", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> addTemplate(
            @PathVariable String projectUrl,
            @Valid @RequestBody CreateJobTemplateRequest request) {
        templateService.add(projectUrl, request.getJobUrl(), request.getName());
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Delete Template")
    @DeleteMapping(value = "/project/{projectUrl}/template/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> deleteTemplate(
            @PathVariable String projectUrl,
            @PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Get Template")
    @GetMapping(value = "/project/{projectUrl}/template/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<JobTemplateVo>> getTemplate(
            @PathVariable String projectUrl,
            @PathVariable Long id) {
        return ResponseEntity.ok(Code.success.asResponse(JobTemplateVo.fromBo(templateService.get(id))));
    }

    @Operation(summary = "Get Templates for project")
    @GetMapping(value = "/project/{projectUrl}/template", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<List<JobTemplateVo>>> selectAllInProject(@PathVariable String projectUrl) {
        return ResponseEntity.ok(Code.success.asResponse(
                templateService.listAll(projectUrl).stream()
                        .map(JobTemplateVo::fromBo)
                        .collect(Collectors.toList())
        ));
    }

    @Operation(summary = "Get Recently Templates for project")
    @GetMapping(value = "/project/{projectUrl}/recent-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<List<JobTemplateVo>>> selectRecentlyInProject(
            @PathVariable String projectUrl,
            @RequestParam(required = false, defaultValue = "5")
            @Valid
            @Min(value = 1, message = "limit must be greater than or equal to 1")
            @Max(value = 50, message = "limit must be less than or equal to 50")
            Integer limit
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                templateService.listRecently(projectUrl, limit).stream()
                        .map(JobTemplateVo::fromBo)
                        .collect(Collectors.toList())
        ));
    }
}
