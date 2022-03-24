/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.report;

import ai.starwhale.mlops.domain.task.EvaluationTask;
import lombok.Data;

import java.util.List;

/**
 * Return Task commands to Agent
 */
@Data
public class ReportResponse {

    List<String> taskIdsToCancel;

    List<EvaluationTask> tasksToRun;

}
