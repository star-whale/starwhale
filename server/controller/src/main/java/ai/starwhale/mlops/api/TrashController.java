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
import ai.starwhale.mlops.api.protocol.trash.TrashVo;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.trash.bo.TrashQuery;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "Trash")
@RequestMapping("${sw.controller.api-prefix}")
public class TrashController {

    private final TrashService trashService;

    public TrashController(TrashService trashService) {
        this.trashService = trashService;
    }

    @Operation(summary = "Get the list of trash",
            description = "List all types of trashes, such as models datasets runtimes and evaluations")
    @GetMapping(value = "/project/{projectUrl}/trash", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<PageInfo<TrashVo>>> listTrash(
            @PathVariable String projectUrl,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        TrashQuery query = TrashQuery.builder()
                .projectUrl(projectUrl)
                .operator(operator)
                .name(name)
                .type(type)
                .build();
        PageInfo<TrashVo> pageinfo = trashService.listTrash(query, PageParams.builder()
                        .pageNum(pageNum)
                        .pageSize(pageSize)
                        .build(),
                OrderParams.builder().build());
        return ResponseEntity.ok(Code.success.asResponse(pageinfo));
    }

    @Operation(summary = "Restore trash by id.",
            description = "Restore a trash to its original type and move it out of the recycle bin."
    )
    @PutMapping(value = "/project/{projectUrl}/trash/{trashId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> recoverTrash(
            @PathVariable String projectUrl,
            @PathVariable Long trashId
    ) {
        boolean res = trashService.recover(projectUrl, trashId);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Recover trash failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Operation(summary = "Delete trash by id.",
            description = "Move a trash out of the recycle bin. This operation cannot be resumed.")
    @DeleteMapping(value = "/project/{projectUrl}/trash/{trashId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER')")
    ResponseEntity<ResponseMessage<String>> deleteTrash(
            @PathVariable String projectUrl,
            @PathVariable Long trashId
    ) {
        boolean res = trashService.deleteTrash(projectUrl, trashId);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Delete trash failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }
}
