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

import static ai.starwhale.mlops.domain.bundle.BundleManager.BUNDLE_NAME_REGEX;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.bundle.DataScope;
import ai.starwhale.mlops.api.protocol.runtime.BuildImageResult;
import ai.starwhale.mlops.api.protocol.runtime.ClientRuntimeRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeInfoVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeRevertRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeTagRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeViewVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.job.spec.RunEnvs;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeQuery;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersion;
import ai.starwhale.mlops.domain.runtime.bo.RuntimeVersionQuery;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Validated
@RestController
@Tag(name = "Runtime")
@RequestMapping("${sw.controller.api-prefix}")
public class RuntimeController {

    private final RuntimeService runtimeService;

    public RuntimeController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Operation(summary = "Get the list of runtimes")
    @GetMapping(value = "/project/{projectUrl}/runtime", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<RuntimeVo>>> listRuntime(
            @PathVariable String projectUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Runtime name prefix to search for")
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        PageInfo<RuntimeVo> pageInfo = runtimeService.listRuntime(
                RuntimeQuery.builder()
                        .projectUrl(projectUrl)
                        .name(name)
                        .owner(owner)
                        .build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build()
        );
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Operation(summary = "List runtime tree including global runtimes")
    @GetMapping(value = "/project/{projectUrl}/runtime-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<RuntimeViewVo>>> listRuntimeTree(
            @PathVariable String projectUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Data range")
            @RequestParam(required = false, defaultValue = "all") DataScope scope
    ) {
        List<RuntimeViewVo> list;
        switch (scope) {
            case all:
                list = runtimeService.listRuntimeVersionView(projectUrl, true, true);
                break;
            case shared:
                list = runtimeService.listRuntimeVersionView(projectUrl, true, false);
                break;
            case project:
                list = runtimeService.listRuntimeVersionView(projectUrl, false, true);
                break;
            default:
                list = List.of();
        }
        return ResponseEntity.ok(Code.success.asResponse(list));
    }

    @GetMapping(value = "/project/{projectUrl}/recent-runtime-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<RuntimeViewVo>>> recentRuntimeTree(
            @PathVariable String projectUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Data limit", schema = @Schema())
            @RequestParam(required = false, defaultValue = "5")
            @Valid
            @Min(value = 1, message = "limit must be greater than or equal to 1")
            @Max(value = 50, message = "limit must be less than or equal to 50")
            Integer limit
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                runtimeService.listRecentlyRuntimeVersionView(projectUrl, limit)
        ));

    }

    @Operation(
            summary = "Revert Runtime version",
            description =
                    "Select a historical version of the runtime and revert the latest version of the current runtime"
                            + " to this version")
    @PostMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/revert",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> revertRuntimeVersion(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @Valid @RequestBody RuntimeRevertRequest revertRequest
    ) {
        Boolean res = runtimeService.revertVersionTo(projectUrl, runtimeUrl, revertRequest.getVersionUrl());
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Revert runtime version failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Delete a runtime")
    @DeleteMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteRuntime(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl
    ) {
        Boolean res = runtimeService.deleteRuntime(
                RuntimeQuery.builder()
                        .projectUrl(projectUrl)
                        .runtimeUrl(runtimeUrl)
                        .build());

        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Delete runtime failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Recover a runtime")
    @PutMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/recover",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> recoverRuntime(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl
    ) {
        Boolean res = runtimeService.recoverRuntime(projectUrl, runtimeUrl);
        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Recover runtime failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));

    }

    @Operation(summary = "Get the information of a runtime",
            description = "Return the information of the latest version of the current runtime")
    @GetMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<RuntimeInfoVo>> getRuntimeInfo(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @RequestParam(required = false) String versionUrl
    ) {
        RuntimeInfoVo runtimeInfo = runtimeService.getRuntimeInfo(
                RuntimeQuery.builder()
                        .projectUrl(projectUrl)
                        .runtimeUrl(runtimeUrl)
                        .runtimeVersionUrl(versionUrl)
                        .build());

        return ResponseEntity.ok(Code.success.asResponse(runtimeInfo));
    }

    @Operation(summary = "Set tag of the runtime version")
    @PutMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> modifyRuntime(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String runtimeVersionUrl,
            @Valid @RequestBody RuntimeTagRequest tagRequest
    ) {
        Boolean res = runtimeService.modifyRuntimeVersion(
                projectUrl,
                runtimeUrl,
                runtimeVersionUrl,
                RuntimeVersion.builder()
                        .versionTag(tagRequest.getTag())
                        .build()
        );

        if (!res) {
            throw new StarwhaleApiException(
                    new SwProcessException(ErrorType.DB, "Modify runtime failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @PostMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> addRuntimeVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl,
            @Valid @RequestBody RuntimeTagRequest runtimeTagRequest
    ) {
        runtimeService.addRuntimeVersionTag(
                projectUrl,
                runtimeUrl,
                versionUrl,
                runtimeTagRequest.getTag(),
                runtimeTagRequest.getForce()
        );
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @GetMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<String>>> listRuntimeVersionTags(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl
    ) {
        var tags = runtimeService.listRuntimeVersionTags(projectUrl, runtimeUrl, versionUrl);
        return ResponseEntity.ok(Code.success.asResponse(tags));
    }

    @DeleteMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag/{tag}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteRuntimeVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl,
            @PathVariable String tag
    ) {
        runtimeService.deleteRuntimeVersionTag(projectUrl, runtimeUrl, versionUrl, tag);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @GetMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/tag/{tag}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Long>> getRuntimeVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String tag
    ) {
        var entity = runtimeService.getRuntimeVersionTag(projectUrl, runtimeUrl, tag);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Code.success.asResponse(entity.getVersionId()));
    }

    @Operation(summary = "Share or unshare the runtime version")
    @PutMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}/shared",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> shareRuntimeVersion(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String runtimeVersionUrl,
            @Parameter(
                    in = ParameterIn.QUERY,
                    required = true,
                    description = "1 or true - shared, 0 or false - unshared",
                    schema = @Schema())
            @RequestParam Boolean shared
    ) {
        runtimeService.shareRuntimeVersion(projectUrl, runtimeUrl, runtimeVersionUrl, shared);
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Get the list of the runtime versions")
    @GetMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/version",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<RuntimeVersionVo>>> listRuntimeVersion(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @Parameter(in = ParameterIn.QUERY, description = "Runtime version name prefix")
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        PageInfo<RuntimeVersionVo> pageInfo = runtimeService.listRuntimeVersionHistory(
                RuntimeVersionQuery.builder()
                        .projectUrl(projectUrl)
                        .runtimeUrl(runtimeUrl)
                        .versionName(name)
                        .build(),
                PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build()
        );
        return ResponseEntity.ok(Code.success.asResponse(pageInfo));
    }

    @Operation(summary = "Create a new runtime version",
            description = "Create a new version of the runtime. "
                    + "The data resources can be selected by uploading the file package or entering the server path.")
    @PostMapping(
            value = "/project/{projectUrl}/runtime/{runtimeName}/version/{versionName}/file",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> upload(
            @PathVariable String projectUrl,
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Runtime name is invalid.") @PathVariable String runtimeName,
            @PathVariable String versionName,
            @RequestPart MultipartFile file,
            ClientRuntimeRequest uploadRequest
    ) {
        uploadRequest.setProject(projectUrl);
        uploadRequest.setRuntime(runtimeName + ":" + versionName);
        runtimeService.upload(file, uploadRequest);
        return ResponseEntity.ok(Code.success.asResponse(""));
    }

    @Operation(summary = "Pull file of a runtime version",
            description = "Pull file of a runtime version. ")
    @GetMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/file",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    void pull(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl,
            HttpServletResponse httpResponse
    ) {
        runtimeService.pull(projectUrl, runtimeUrl, versionUrl, httpResponse);
    }

    @Operation(summary = "head for runtime info ",
            description = "head for runtime info")
    @RequestMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<?> headRuntime(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl
    ) {
        try {
            runtimeService.query(projectUrl, runtimeUrl, versionUrl);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("Head runtime result: NOT FOUND");
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "build image for runtime", description = "build image for runtime")
    @RequestMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image/build",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<BuildImageResult>> buildRuntimeImage(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl,
            @Parameter(description = "user defined running configurations such environment variables")
            @RequestBody(required = false) RunEnvs runEnvs
    ) {
        BuildImageResult res = runtimeService.dockerize(projectUrl, runtimeUrl, versionUrl, runEnvs);
        return ResponseEntity.ok(Code.success.asResponse(res));
    }

    @Operation(summary = "update image for runtime", description = "update image for runtime")
    @RequestMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.PUT)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<?>> updateRuntime(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl,
            @Parameter(description = "the image used for this runtime") String runtimeImage
    ) {
        runtimeService.updateImage(projectUrl, runtimeUrl, versionUrl, runtimeImage);
        return ResponseEntity.ok(Code.success.asResponse(null));
    }
}
