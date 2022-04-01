/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.reporting;

import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;

/**
 * the processor for every report from Agent
 */
public interface ReportProcessor {

     /**
      * process the report from Agent
      * @param report request protocol between Agent and Controller
      * @return response protocol between Agent and Controller
      */
     ReportResponse receive(ReportRequest report);

}
