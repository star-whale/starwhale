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
import ai.starwhale.mlops.api.protocol.datastore.FlushRequest;
import ai.starwhale.mlops.api.protocol.datastore.ListTablesRequest;
import ai.starwhale.mlops.api.protocol.datastore.QueryTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.RecordListVo;
import ai.starwhale.mlops.api.protocol.datastore.ScanTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.TableNameListVo;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Validated
public interface DataStoreApi {

    @PostMapping(value = "/datastore/listTables")
    @PreAuthorize("hasAnyRole('GUEST', 'OWNER', 'MAINTAINER', 'ANONYMOUS')")
    ResponseEntity<ResponseMessage<TableNameListVo>> listTables(
            @Valid @RequestBody ListTablesRequest request);

    @PostMapping(value = "/datastore/updateTable")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> updateTable(
            @Valid @RequestBody UpdateTableRequest request);

    @PostMapping(value = "/datastore/flush")
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> flush(FlushRequest request);

    @PostMapping(value = "/datastore/queryTable")
    @PreAuthorize("hasAnyRole('GUEST', 'OWNER', 'MAINTAINER', 'ANONYMOUS')")
    ResponseEntity<ResponseMessage<RecordListVo>> queryTable(
            @Valid @RequestBody QueryTableRequest request);

    @PostMapping(value = "/datastore/scanTable")
    @PreAuthorize("hasAnyRole('GUEST', 'OWNER', 'MAINTAINER', 'ANONYMOUS')")
    ResponseEntity<ResponseMessage<RecordListVo>> scanTable(
            @Valid @RequestBody ScanTableRequest request);

    @PostMapping(value = "/datastore/queryTable/export")
    @PreAuthorize("hasAnyRole('GUEST', 'OWNER', 'MAINTAINER', 'ANONYMOUS')")
    void queryAndExport(
            @Valid @RequestBody QueryTableRequest request, HttpServletResponse httpResponse);

    @PostMapping(value = "/datastore/scanTable/export")
    @PreAuthorize("hasAnyRole('GUEST', 'OWNER', 'MAINTAINER', 'ANONYMOUS')")
    void scanAndExport(
            @Valid @RequestBody ScanTableRequest request, HttpServletResponse httpResponse);
}
