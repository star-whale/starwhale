/*
 * Copyright 2022.1-2022
 * StarWhale.com All right reserved. This software is the confidential and proprietary information of
 * StarWhale.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api.protocol.agent;

import ai.starwhale.mlops.domain.node.Node;
import lombok.Data;

/**
 * Agent report the info of it's node to Controller.
 */
@Data
public class ReportingRequest {
    Node nodeInfo;
}
