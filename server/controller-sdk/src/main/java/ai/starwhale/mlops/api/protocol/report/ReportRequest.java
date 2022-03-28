/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.report;

import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.Task;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Agent report the info of it's node to Controller.
 */
@Data
@Builder
public class ReportRequest {
    Node nodeInfo;
    List<Task> tasks;
}
