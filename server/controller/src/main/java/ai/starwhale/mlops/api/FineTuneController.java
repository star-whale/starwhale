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
import ai.starwhale.mlops.api.protocol.ft.FineTuneCreateRequest;
import ai.starwhale.mlops.api.protocol.ft.FineTuneSpaceCreateRequest;
import ai.starwhale.mlops.api.protocol.ft.FineTuneSpaceVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.ft.FineTuneAppService;
import ai.starwhale.mlops.domain.ft.FineTuneSpaceService;
import ai.starwhale.mlops.domain.ft.vo.FineTuneVo;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.user.UserService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@Tag(name = "FineTune")
@RequestMapping("${sw.controller.api-prefix}")
public class FineTuneController {

    final ProjectService projectService;
    final UserService userService;
    final FineTuneSpaceService fineTuneSpaceService;

    final FineTuneAppService fineTuneAppService;

    public FineTuneController(
            ProjectService projectService,
            UserService userService,
            FineTuneSpaceService fineTuneSpaceService,
            FineTuneAppService fineTuneAppService
    ) {
        this.projectService = projectService;
        this.userService = userService;
        this.fineTuneSpaceService = fineTuneSpaceService;
        this.fineTuneAppService = fineTuneAppService;
    }

    @Operation(summary = "Get the list of fine-tune spaces")
    @GetMapping(value = "/project/{projectId}/ftspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<PageInfo<FineTuneSpaceVo>>> listSpace(
            @PathVariable("projectId") Long projectId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        PageInfo<FineTuneSpaceVo> pageInfo = fineTuneSpaceService.listSpace(projectId, pageNum, pageSize);
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }


    @Operation(summary = "Create fine-tune space")
    @PostMapping(value = "/project/{projectId}/ftspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> createSpace(
            @PathVariable("projectId") Long projectId,
            @RequestBody FineTuneSpaceCreateRequest body
    ) {
        fineTuneSpaceService.createSpace(
                projectId,
                body.getName(),
                body.getDescription(),
                new IdConverter().revert(userService.currentUser().getId())
        );
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Operation(summary = "Update fine-tune space")
    @PutMapping(value = "/project/{projectId}/ftspace/{spaceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> updateSpace(
            @PathVariable("projectId") Long projectId,
            @PathVariable("spaceId") Long spaceId,
            @RequestBody FineTuneSpaceCreateRequest body
    ) {
        fineTuneSpaceService.updateSpace(
                spaceId,
                body.getName(),
                body.getDescription()
        );
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Operation(summary = "Create fine-tune")
    @PostMapping(value = "/project/{projectId}/ftspace/{spaceId}/ft", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> createFineTune(
            @PathVariable("projectId") Long projectId,
            @PathVariable("spaceId") Long spaceId,
            @Valid @RequestBody FineTuneCreateRequest request
    ) {

        fineTuneAppService.createFineTune(
                spaceId,
                projectService.findProject(projectId),
                request,
                userService.currentUserDetail()

        );
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Operation(summary = "List fine-tune")
    @GetMapping(value = "/project/{projectId}/ftspace/{spaceId}/ft", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<PageInfo<FineTuneVo>>> listFineTune(
            @PathVariable("projectId") Long projectId,
            @PathVariable("spaceId") Long spaceId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {

        PageInfo<FineTuneVo> pageInfo = fineTuneAppService.list(spaceId, pageNum, pageSize);
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }
}
