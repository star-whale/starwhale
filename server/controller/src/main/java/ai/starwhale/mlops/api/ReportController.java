/**
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
import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;
import ai.starwhale.mlops.reporting.ReportProcessor;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class ReportController implements ReportApi{

    final ReportProcessor reportProcessor;

    @Resource
    private TaskLogWSServer taskLogWSServer;

    public ReportController(ReportProcessor reportProcessor) {
        this.reportProcessor = reportProcessor;
    }

    @PostMapping("report")
    @Override
    public ResponseMessage<ReportResponse> report(@RequestBody ReportRequest request) {
        reportLog(request.getTasks());

        ReportResponse reportResponse = reportProcessor.receive(request);
        reportResponse.setLogReaders(taskLogWSServer.getLogReaders());
        return Code.success.asResponse(reportResponse);
    }

    private void reportLog(List<TaskReport> taskReports) {
        if(taskReports != null) {
            for (TaskReport taskReport : taskReports) {
                taskLogWSServer.report(taskReport);
            }
        }
    }
}
