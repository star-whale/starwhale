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
import ai.starwhale.mlops.api.protocol.report.CreateReportRequest;
import ai.starwhale.mlops.api.protocol.report.ReportVo;
import ai.starwhale.mlops.api.protocol.report.TransferReportRequest;
import ai.starwhale.mlops.api.protocol.report.UpdateReportRequest;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.report.ReportService;
import ai.starwhale.mlops.domain.report.bo.CreateParam;
import ai.starwhale.mlops.domain.report.bo.QueryParam;
import ai.starwhale.mlops.domain.report.bo.UpdateParam;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "Report")
@RequestMapping("${sw.controller.api-prefix}")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }


    @PostMapping(value = "/project/{projectUrl}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> createReport(
            @PathVariable String projectUrl,
            @Valid @RequestBody CreateReportRequest createReportRequest
    ) {
        Long id = service.create(CreateParam.builder()
                .projectUrl(projectUrl)
                .title(createReportRequest.getTitle())
                .description(createReportRequest.getDescription())
                .content(createReportRequest.getContent())
                .build());
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(id)));
    }

    @PostMapping(value = "/project/{projectUrl}/report/{reportId}/transfer",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> transfer(
            @PathVariable String projectUrl,
            @PathVariable Long reportId,
            @Valid @RequestBody TransferReportRequest transferRequest
    ) {
        return null;
    }

    @PutMapping(value = "/project/{projectUrl}/report/{reportId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> modifyReport(
            @PathVariable String projectUrl,
            @PathVariable Long reportId,
            @Valid @RequestBody UpdateReportRequest updateReportRequest
    ) {
        service.update(UpdateParam.builder()
                .reportId(reportId)
                .projectUrl(projectUrl)
                .content(updateReportRequest.getContent())
                .description(updateReportRequest.getDescription())
                .title(updateReportRequest.getTitle())
                .shared(updateReportRequest.getShared())
                .build());
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @PutMapping(value = "/project/{projectUrl}/report/{reportId}/shared", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> sharedReport(
            @PathVariable String projectUrl,
            @PathVariable Long reportId,
            @RequestParam Boolean shared
    ) {
        service.shared(UpdateParam.builder().reportId(reportId).shared(shared).build());
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @DeleteMapping(value = "/project/{projectUrl}/report/{reportId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER')")
    ResponseEntity<ResponseMessage<String>> deleteReport(
            @PathVariable String projectUrl,
            @PathVariable Long reportId
    ) {
        service.delete(QueryParam.builder()
                .projectUrl(projectUrl).reportId(reportId).build());
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @GetMapping(value = "/project/{projectUrl}/report/{reportId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<ReportVo>> getReport(
            @PathVariable String projectUrl,
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(Code.success.asResponse(
                service.getReport(QueryParam.builder()
                        .projectUrl(projectUrl)
                        .reportId(reportId)
                        .build())));
    }

    @GetMapping(value = "/report/{uuid}/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('GUEST', 'OWNER', 'MAINTAINER', 'ANONYMOUS')")
    ResponseEntity<ResponseMessage<ReportVo>> preview(
            @PathVariable String uuid
    ) {
        return ResponseEntity.ok(Code.success.asResponse(service.getReportByUuidForPreview(uuid)));
    }

    @Operation(summary = "Get the list of reports")
    @GetMapping(value = "/project/{projectUrl}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MAINTAINER', 'GUEST')")
    ResponseEntity<ResponseMessage<PageInfo<ReportVo>>> listReports(
            @PathVariable String projectUrl,
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return ResponseEntity.ok(Code.success.asResponse(service.listReport(
                QueryParam.builder().title(title).projectUrl(projectUrl).build(),
                PageParams.builder().pageNum(pageNum).pageSize(pageSize).build()
        )));
    }
}
