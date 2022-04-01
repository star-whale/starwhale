/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;

/**
 * report api:the expose api by the controller
 */
public interface ReportApi {
    /**
     * report agent info
     * @param request the agent info: include machine's info and task's status
     * @return return a sequence of commands for the agent, such as the tasks to run and the tasks to cancel.
     */
    ResponseMessage<ReportResponse> report(ReportRequest request);
}
