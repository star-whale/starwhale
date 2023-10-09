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
import ai.starwhale.mlops.domain.evaluation.EvaluationFileStorage;
import ai.starwhale.mlops.domain.evaluation.EvaluationService;
import ai.starwhale.mlops.domain.evaluation.bo.ConfigQuery;
import ai.starwhale.mlops.domain.evaluation.bo.SummaryFilter;
import ai.starwhale.mlops.domain.storage.HashNamedObjectStore;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@Tag(name = "Evaluation")
@Validated
@RequestMapping("${sw.controller.api-prefix}")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final EvaluationFileStorage evaluationFileStorage;

    public EvaluationController(EvaluationService evaluationService, EvaluationFileStorage evaluationFileStorage) {
        this.evaluationService = evaluationService;
        this.evaluationFileStorage = evaluationFileStorage;
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

    @Operation(summary = "Upload a hashed BLOB to evaluation object store",
            description = "Upload a hashed BLOB to evaluation object store, returns a uri of the main storage")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/evaluation/{version}/hashedBlob/{hash}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> uploadHashedBlob(
            @PathVariable(name = "projectUrl") String projectUrl,
            @PathVariable(name = "version") String version,
            @PathVariable(name = "hash") String hash,
            @Parameter(description = "file content") @RequestPart(value = "file", required = true)
            MultipartFile file
    ) {
        return ResponseEntity.ok(
                Code.success.asResponse(evaluationFileStorage.uploadHashedBlob(projectUrl, version, file, hash)));
    }

    @Operation(summary = "Test if a hashed blob exists in this evaluation",
            description = "404 if not exists; 200 if exists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @RequestMapping(
            value = "/project/{projectUrl}/evaluation/{version}/hashedBlob/{hash}",
            method = RequestMethod.HEAD,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<?> headHashedBlob(
            @PathVariable(name = "projectUrl") String projectUrl,
            @PathVariable(name = "version") String version,
            @PathVariable(name = "hash") String hash
    ) {
        HashNamedObjectStore hashNamedObjectStore = evaluationFileStorage.hashObjectStore(projectUrl, version);
        String path;
        try {
            path = hashNamedObjectStore.head(hash);
        } catch (IOException e) {
            log.error("access to main object storage failed", e);
            throw new SwProcessException(ErrorType.STORAGE, "access to main object storage failed", e);
        }
        if (null != path) {
            return ResponseEntity.ok().header("X-SW-LOCAL-STORAGE-URI", path).build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Download the hashed blob in this evaluation",
            description = "404 if not exists; 200 if exists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @RequestMapping(
            value = "/project/{projectUrl}/evaluation/{version}/hashedBlob/{hash}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    void getHashedBlob(
            @PathVariable(name = "projectUrl") String projectUrl,
            @PathVariable(name = "version") String version,
            @PathVariable(name = "hash") String hash,
            HttpServletResponse httpResponse
    ) {
        try (
                var inputStream = evaluationFileStorage.hashObjectStore(projectUrl, version).get(hash.trim());
                var outputStream = httpResponse.getOutputStream()
        ) {
            httpResponse.addHeader("Content-Disposition", "attachment; filename=\"" + hash + "\"");
            httpResponse.addHeader("Content-Length", String.valueOf(inputStream.getSize()));
            inputStream.transferTo(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE, "pull file from storage failed", e);
        }
    }


    @Operation(summary = "Sign uris to get a batch of temporarily accessible links",
            description = "Sign uris to get a batch of temporarily accessible links")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PostMapping(
            value = "/project/{projectUrl}/evaluation/{version}/uri/sign-links",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<Map<String, String>>> signLinks(
            @PathVariable(name = "projectUrl") String projectUrl,
            @PathVariable(name = "version") String version,
            @RequestBody Set<String> uris,
            @Parameter(name = "expTimeMillis", description = "the link will be expired after expTimeMillis")
            @RequestParam(name = "expTimeMillis", required = false)
            Long expTimeMillis
    ) {
        return ResponseEntity.ok(Code.success.asResponse(evaluationFileStorage.signLinks(uris, expTimeMillis)));
    }
}
