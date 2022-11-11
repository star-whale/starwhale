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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class TrashController implements TrashApi {

    private final TrashService trashService;

    public TrashController(TrashService trashService) {
        this.trashService = trashService;
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<TrashVo>>> listTrash(String projectUrl, String name,
            String operator, String type, Integer pageNum, Integer pageSize) {
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

    @Override
    public ResponseEntity<ResponseMessage<String>> recoverTrash(String projectUrl, Long trashId) {
        boolean res = trashService.recover(projectUrl, trashId);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Recover trash failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> deleteTrash(String projectUrl, Long trashId) {
        boolean res = trashService.deleteTrash(projectUrl, trashId);
        if (!res) {
            throw new StarwhaleApiException(new SwProcessException(ErrorType.DB, "Delete trash failed."),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }
}
