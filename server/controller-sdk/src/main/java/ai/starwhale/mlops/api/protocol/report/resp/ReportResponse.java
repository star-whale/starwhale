/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.report.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Return Task commands to Agent
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportResponse {

    List<Long> taskIdsToCancel;

    List<TaskTrigger> tasksToRun;

    List<LogReader> logReaders;
}
