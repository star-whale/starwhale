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
import ai.starwhale.mlops.api.protocol.filestorage.ApplySignedUrlRequest;
import ai.starwhale.mlops.api.protocol.filestorage.FileDeleteRequest;
import ai.starwhale.mlops.api.protocol.filestorage.SignedUrlResponse;
import ai.starwhale.mlops.domain.filestorage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Tag(name = "File storage", description = "File storage operations")
@RequestMapping("${sw.controller.api-prefix}")
public class FileStorageController {

    private final FileStorageService service;

    public FileStorageController(FileStorageService service) {
        this.service = service;
    }

    @Operation(summary = "Apply pathPrefix", description = "Apply pathPrefix")
    @GetMapping("/filestorage/path/apply")
    ResponseEntity<ResponseMessage<String>> applyPathPrefix(String flag) {
        return ResponseEntity.ok(Code.success.asResponse(service.generatePathPrefix(flag)));
    }

    @Operation(summary = "Apply signedUrls for put", description = "Apply signedUrls for put")
    @PutMapping("/filestorage/signedurl")
    ResponseEntity<ResponseMessage<SignedUrlResponse>> applySignedPutUrls(
            @RequestBody ApplySignedUrlRequest applySignedUrlRequest
    ) {
        var path = applySignedUrlRequest.getPathPrefix();
        var signedUrls = service.generateSignedPutUrls(path, applySignedUrlRequest.getFiles());

        return ResponseEntity.ok(Code.success.asResponse(new SignedUrlResponse(path, signedUrls)));
    }

    @Operation(summary = "Apply signedUrls for get", description = "Apply signedUrls for get")
    @GetMapping("/filestorage/signedurl")
    ResponseEntity<ResponseMessage<SignedUrlResponse>> applySignedGetUrls(String pathPrefix) {
        return ResponseEntity.ok(Code.success.asResponse(
                new SignedUrlResponse(pathPrefix, service.generateSignedGetUrls(pathPrefix))));
    }

    @Operation(summary = "Delete path", description = "Delete path")
    @DeleteMapping("/filestorage/file")
    ResponseEntity<ResponseMessage<String>> deletePath(@RequestBody FileDeleteRequest request) {
        service.deleteFiles(request.getPathPrefix(), request.getFiles());
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }
}
