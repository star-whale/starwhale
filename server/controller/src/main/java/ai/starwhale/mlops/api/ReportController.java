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
import ai.starwhale.mlops.exception.SwValidationException;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

@Tag(name = "Report")
@Validated
public class ReportController implements ReportApi {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }


    public ResponseEntity<ResponseMessage<String>> createReport(String projectUrl, CreateReportRequest createReportRequest) {
        Long id = service.create(CreateParam.builder()
                .projectUrl(projectUrl)
                .name(createReportRequest.getName())
                .content(createReportRequest.getContent())
                .build());
        return ResponseEntity.ok(Code.success.asResponse(String.valueOf(id)));
    }

    public ResponseEntity<ResponseMessage<String>> transfer(String projectUrl, String reportUrl, TransferReportRequest transferRequest) {
        return null;
    }

    public ResponseEntity<ResponseMessage<String>> modifyReport(
            String projectUrl, String reportUrl, UpdateReportRequest updateReportRequest) {
        if (updateReportRequest.validate()) {
            throw new SwValidationException(SwValidationException.ValidSubject.REPORT);
        }
        service.update(UpdateParam.builder()
                .reportUrl(reportUrl)
                .projectUrl(projectUrl)
                .content(updateReportRequest.getContent())
                .shared(updateReportRequest.getShared())
                .build());
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    public ResponseEntity<ResponseMessage<String>> deleteReport(String projectUrl, String reportUrl) {
        service.delete(QueryParam.builder()
                .projectUrl(projectUrl).reportUrl(reportUrl).build());
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    public ResponseEntity<ResponseMessage<ReportVo>> getReport(String projectUrl, String reportUrl) {
        return ResponseEntity.ok(Code.success.asResponse(
                service.getReport(QueryParam.builder()
                        .projectUrl(projectUrl)
                        .reportUrl(reportUrl)
                        .build())));
    }

    public ResponseEntity<ResponseMessage<PageInfo<ReportVo>>> listReports(
            String projectUrl, Integer pageNum, Integer pageSize) {
        return ResponseEntity.ok(Code.success.asResponse(service.listReport(
                QueryParam.builder().projectUrl(projectUrl).build(),
                PageParams.builder().build())));
    }
}
