/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.swds.SWDataSetSlice;
import ai.starwhale.mlops.domain.swds.index.SWDSBlock;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * sufficient information for an Agent to run a Task
 */
@Data
@Builder
public class TaskTrigger {

    /**
     * task meta info
     */
    private Task task;

    /**
     * the proper image to get swmp run
     */
    private String imageId;

    /**
     * swmp meta info
     */
    private SWModelPackage swModelPackage;

    /**
     * @deprecated
     * swds slice meta info
     */
    private List<SWDataSetSlice> swDataSetSlice;

    /**
     * blocks may come from different SWDS
     */
    private List<SWDSBlock> swdsBlocks;

    public boolean equals(Object obj){

        if(!(obj instanceof TaskTrigger)){
            return false;
        }
        TaskTrigger tt = (TaskTrigger)obj;
        if(null == tt.getTask() || null == tt.getTask().getId()){
            return false;
        }
        return this.task.getId().equals(tt.getTask().getId());
    }
}
