/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.swds.SWDataSetSlice;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * sufficient information for an Agent to run a Task
 */
@Data
@Builder
public class TaskTrigger {

    /**
     * task meta info
     */
    Task task;

    /**
     * swmp meta info
     */
    SWModelPackage swModelPackage;

    /**
     * swds slice meta info
     */
    List<SWDataSetSlice> swDataSetSlice;
}
