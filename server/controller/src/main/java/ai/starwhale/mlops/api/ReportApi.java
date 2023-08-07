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
import ai.starwhale.mlops.api.protocol.report.CreateReportRequest;
import ai.starwhale.mlops.api.protocol.report.ReportVo;
import ai.starwhale.mlops.api.protocol.report.TransferReportRequest;
import ai.starwhale.mlops.api.protocol.report.UpdateReportRequest;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;


@Tag(name = "Report")
@Validated
public interface ReportApi {

    @PostMapping(value = "/project/{projectUrl}/report",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> createReport(
        @PathVariable String projectUrl,
        @Valid @RequestBody CreateReportRequest createReportRequest);

    @PostMapping(value = "/project/{projectUrl}/report/{reportUrl}/transfer",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> transfer(
        @PathVariable String projectUrl,
        @PathVariable String reportUrl,
        @Valid @RequestBody TransferReportRequest transferRequest);

    @PutMapping(value = "/project/{projectUrl}/report/{reportUrl}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> modifyReport(
        @PathVariable String projectUrl,
        @PathVariable String reportUrl,
        @Valid @RequestBody UpdateReportRequest updateReportRequest);



    @DeleteMapping(value = "/project/{projectUrl}/report/{reportUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteReport(
        @PathVariable String projectUrl,
        @PathVariable String reportUrl);

    @GetMapping(value = "/project/{projectUrl}/report/{reportUrl}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<ReportVo>> getReport(
        @PathVariable String projectUrl,
        @PathVariable String reportUrl);


    @Operation(summary = "Get the list of reports")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "ok")})
    @GetMapping(
        value = "/project/{projectUrl}/report",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<ReportVo>>> listReports(
        @PathVariable String projectUrl,
        @Valid @RequestParam(required = false, defaultValue = "1") Integer pageNum,
        @Valid @RequestParam(required = false, defaultValue = "10") Integer pageSize
    );
}
