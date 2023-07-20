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
import ai.starwhale.mlops.api.protocol.filestorage.ApplySignedUrlRequest;
import ai.starwhale.mlops.api.protocol.filestorage.FileDeleteRequest;
import ai.starwhale.mlops.api.protocol.filestorage.SignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "File storage", description = "File storage operations")
@Validated
public interface FileStorageApi {

    @Operation(summary = "Apply pathPrefix", description = "Apply pathPrefix")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping("/filestorage/path/apply")
    ResponseEntity<ResponseMessage<String>> applyPathPrefix(String flag);

    @Operation(summary = "Apply signedUrls for put", description = "Apply signedUrls for put")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping("/filestorage/signedurl")
    ResponseEntity<ResponseMessage<SignedUrlResponse>> applySignedPutUrls(
            @RequestBody ApplySignedUrlRequest applySignedUrlRequest);

    @Operation(summary = "Apply signedUrls for get", description = "Apply signedUrls for get")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping("/filestorage/signedurl")
    ResponseEntity<ResponseMessage<SignedUrlResponse>> applySignedGetUrls(String pathPrefix);

    @Operation(summary = "Delete path", description = "Delete path")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping("/filestorage/file")
    ResponseEntity<ResponseMessage<String>> deletePath(@RequestBody FileDeleteRequest request);

}
