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
import ai.starwhale.mlops.api.protocol.trash.TrashVo;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Trash")
@Validated
public interface TrashApi {

    @Operation(summary = "Get the list of trash")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "ok",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageInfo.class)))})
    @GetMapping(value = "/project/{projectUrl}/trash")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<PageInfo<TrashVo>>> listTrash(
            @PathVariable(value = "projectUrl") String projectUrl,
            @Valid @RequestParam(value = "name", required = false) String name,
            @Valid @RequestParam(value = "operator", required = false) String operator,
            @Valid @RequestParam(value = "type", required = false) String type,
            @Valid @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @Valid @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize);


    @Operation(summary = "Recover trash by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ok")})
    @PutMapping(value = "/project/{projectUrl}/trash/{trashId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> recoverTrash(
            @PathVariable(value = "projectUrl") String projectUrl,
            @PathVariable(value = "trashId") Long trashId
    );

    @Operation(summary = "Delete trash")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ok")})
    @DeleteMapping(value = "/project/{projectUrl}/trash/{trashId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteTrash(
            @PathVariable(value = "projectUrl") String projectUrl,
            @PathVariable(value = "trashId") Long trashId
    );
}
