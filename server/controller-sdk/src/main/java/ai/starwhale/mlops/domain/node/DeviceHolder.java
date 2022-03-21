/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.node;

import ai.starwhale.mlops.domain.task.Task;
import lombok.Data;

/**
 * When a Task is running on a Device we call it "a Device is held by a task"
 * Commonly, a Device can't be held by more than one Task
 */
@Data
public class DeviceHolder {

    /**
     * the Device that is held
     */
    Device device;

    /**
     * the Task which is holding the Device
     */
    Task holder;
}
