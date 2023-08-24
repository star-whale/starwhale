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

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.runtime.BuildImageResult;
import ai.starwhale.mlops.api.protocol.runtime.ClientRuntimeRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeInfoVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeRevertRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeTagRequest;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeViewVo;
import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.domain.job.spec.RunEnvs;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
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
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Runtime")
@Validated
public interface RuntimeApi {

    @Operation(summary = "Get the list of runtimes")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/runtime",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<RuntimeVo>>> listRuntime(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    required = true,
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "Runtime name prefix to search for",
                    schema = @Schema())
            @Valid @RequestParam(value = "name", required = false) String name,
            @Valid @RequestParam(value = "owner", required = false) String owner,
            @Parameter(in = ParameterIn.QUERY, description = "Page number", schema = @Schema())
            @Valid
            @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
            @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
            @Valid
            @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

    @Operation(
            summary = "Revert Runtime version",
            description =
                    "Select a historical version of the runtime and revert the latest version of the current runtime"
                            + " to this version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/revert",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> revertRuntimeVersion(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    required = true,
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Runtime Url",
                    required = true,
                    schema = @Schema())
            @PathVariable("runtimeUrl")
            String runtimeUrl,
            @Valid @RequestBody RuntimeRevertRequest revertRequest);

    @Operation(summary = "Delete a runtime")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteRuntime(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeUrl")
            String runtimeUrl);

    @Operation(summary = "Recover a runtime")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/recover",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> recoverRuntime(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeUrl") String runtimeUrl);

    @Operation(summary = "Get the information of a runtime",
            description = "Return the information of the latest version of the current runtime")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<RuntimeInfoVo>> getRuntimeInfo(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeUrl") String runtimeUrl,
            @RequestParam(value = "versionUrl", required = false) String versionUrl);

    @Operation(summary = "Set tag of the runtime version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> modifyRuntime(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeUrl") String runtimeUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeVersionUrl") String runtimeVersionUrl,
            @Valid @RequestBody RuntimeTagRequest tagRequest);

    @PostMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> addRuntimeVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl,
            @Valid @RequestBody RuntimeTagRequest runtimeTagRequest);

    @GetMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<String>>> listRuntimeVersionTags(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl);

    @DeleteMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/tag/{tag}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteRuntimeVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String versionUrl,
            @PathVariable String tag);

    @GetMapping(value = "/project/{projectUrl}/runtime/{runtimeUrl}/tag/{tag}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Long>> getRuntimeVersionTag(
            @PathVariable String projectUrl,
            @PathVariable String runtimeUrl,
            @PathVariable String tag);

    @Operation(summary = "Share or unshare the runtime version")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{runtimeVersionUrl}/shared",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> shareRuntimeVersion(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeUrl") String runtimeUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeVersionUrl") String runtimeVersionUrl,
            @Parameter(
                    in = ParameterIn.QUERY,
                    required = true,
                    description = "1 or true - shared, 0 or false - unshared",
                    schema = @Schema())
            @RequestParam(value = "shared") Boolean shared
    );

    @Operation(summary = "Get the list of the runtime versions")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<RuntimeVersionVo>>> listRuntimeVersion(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Project Url",
                    required = true,
                    schema = @Schema())
            @PathVariable("projectUrl")
            String projectUrl,
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "Runtime Url",
                    required = true,
                    schema = @Schema())
            @PathVariable("runtimeUrl")
            String runtimeUrl,
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "Runtime version name prefix",
                    schema = @Schema())
            @RequestParam(value = "name", required = false)
            String name,
            @Parameter(in = ParameterIn.QUERY, description = "The page number", schema = @Schema())
            @Valid
            @RequestParam(value = "pageNum", required = false, defaultValue = "1")
            Integer pageNum,
            @Parameter(in = ParameterIn.QUERY, description = "Rows per page", schema = @Schema())
            @Valid
            @RequestParam(value = "pageSize", required = false, defaultValue = "10")
            Integer pageSize);

    @Operation(summary = "List runtime tree including global runtimes")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/runtime-tree",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<List<RuntimeViewVo>>> listRuntimeTree(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl);

    @Operation(summary = "Create a new runtime version",
            description = "Create a new version of the runtime. "
                    + "The data resources can be selected by uploading the file package or entering the server path.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/runtime/{runtimeName}/version/{versionName}/file",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> upload(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @Pattern(regexp = BUNDLE_NAME_REGEX, message = "Runtime name is invalid.")
            @PathVariable("runtimeName") String runtimeName,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionName") String versionName,
            @Parameter(description = "file detail")
            @RequestPart(value = "file") MultipartFile file,
            ClientRuntimeRequest uploadRequest);

    @Operation(summary = "Pull file of a runtime version",
            description = "Pull file of a runtime version. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/file",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    void pull(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeUrl") String runtimeUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl") String versionUrl,
            HttpServletResponse httpResponse);


    @Operation(summary = "head for runtime info ",
            description = "head for runtime info")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @RequestMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<?> headRuntime(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeUrl") String runtimeUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl") String versionUrl);

    @Operation(summary = "build image for runtime", description = "build image for runtime")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @RequestMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image/build",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<BuildImageResult>> buildRuntimeImage(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeUrl") String runtimeUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl") String versionUrl,
            @Parameter(description = "user defined running configurations such environment variables")
            @RequestBody(required = false) RunEnvs runEnvs);

    @Operation(summary = "update image for runtime", description = "update image for runtime")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @RequestMapping(
            value = "/project/{projectUrl}/runtime/{runtimeUrl}/version/{versionUrl}/image",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.PUT)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<?>> updateRuntime(
            @Parameter(in = ParameterIn.PATH, required = true, description = "Project url", schema = @Schema())
            @PathVariable("projectUrl") String projectUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("runtimeUrl") String runtimeUrl,
            @Parameter(in = ParameterIn.PATH, required = true, schema = @Schema())
            @PathVariable("versionUrl") String versionUrl,
            @Parameter(description = "the image used for this runtime")
            String runtimeImage);


}
