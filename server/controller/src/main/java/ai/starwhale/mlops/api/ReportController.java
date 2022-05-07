/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
