/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.report.req;

import ai.starwhale.mlops.domain.node.Node;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent report the info of it's node to Controller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    Node nodeInfo;
    List<TaskReport> tasks;
}
