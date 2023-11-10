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
import ai.starwhale.mlops.api.protocol.sft.SftCreateRequest;
import ai.starwhale.mlops.api.protocol.sft.SftSpaceCreateRequest;
import ai.starwhale.mlops.api.protocol.sft.SftSpaceVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.sft.SftService;
import ai.starwhale.mlops.domain.sft.SftSpaceService;
import ai.starwhale.mlops.domain.sft.vo.SftVo;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Tag(name = "Sft")
@RequestMapping("${sw.controller.api-prefix}")
public class SftController {

    final ProjectService projectService;
    final UserService userService;
    final SftSpaceService sftSpaceService;

    final SftService sftService;

    public SftController(
            ProjectService projectService,
            UserService userService,
            SftSpaceService sftSpaceService,
            SftService sftService
    ) {
        this.projectService = projectService;
        this.userService = userService;
        this.sftSpaceService = sftSpaceService;
        this.sftService = sftService;
    }

    @Operation(summary = "Get the list of SFT spaces")
    @GetMapping(value = "/project/{projectId}/sft/space", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<PageInfo<SftSpaceVo>>> listSftSpace(
            @PathVariable("projectId") Long projectId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        PageInfo<SftSpaceVo> pageInfo = sftSpaceService.listSpace(projectId, pageNum, pageSize);
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }


    @Operation(summary = "Create SFT space")
    @PostMapping(value = "/project/{projectId}/sft/space", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> createSftSpace(
            @PathVariable("projectId") Long projectId,
            @RequestBody SftSpaceCreateRequest body
    ) {
        sftSpaceService.createSpace(
                projectId,
                body.getName(),
                body.getDescription(),
                new IdConverter().revert(userService.currentUser().getId())
        );
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Operation(summary = "Create SFT")
    @PostMapping(value = "/project/{projectId}/sft/space/{spaceId}/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    public ResponseEntity<ResponseMessage<String>> createSft(
            @PathVariable("projectId") Long projectId,
            @PathVariable("spaceId") Long spaceId,
            @Valid @RequestBody SftCreateRequest request
    ) {

        sftService.createSft(
                spaceId,
                projectService.findProject(projectId),
                request,
                userService.currentUserDetail()

        );
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Operation(summary = "List SFT")
    @GetMapping(value = "/project/{projectId}/sft/space/{spaceId}/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    public ResponseEntity<ResponseMessage<PageInfo<SftVo>>> listSft(
            @PathVariable("projectId") Long projectId,
            @PathVariable("spaceId") Long spaceId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {

        PageInfo<SftVo> pageInfo = sftService.listSft(spaceId, pageNum, pageSize);
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }
}
