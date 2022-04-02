/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.gpu;

import java.util.List;
import java.util.Optional;

/**
 * detect devices of the machine
 */
public interface GPUDetect {
    Optional<List<GPUInfo>> detect();
}
