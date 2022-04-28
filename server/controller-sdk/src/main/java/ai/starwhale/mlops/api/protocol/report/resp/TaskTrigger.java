/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.report.resp;

import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.swds.index.SWDSBlock;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import java.util.List;

import ai.starwhale.mlops.domain.task.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * sufficient information for an Agent to run a Task
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskTrigger {

    /**
     * unique id for the task
     */
    Long id;

    TaskType taskType;

    /**
     * input information at resulting stage: CMP file path
     */
    List<String> cmpInputFilePaths;

    /**
     * the proper image to get swmp run
     */
    private String imageId;

    /**
     * swmp meta info
     */
    private SWModelPackage swModelPackage;

    /**
     * blocks may come from different SWDS
     */
    private List<SWDSBlock> swdsBlocks;

    private Integer deviceAmount;

    private Device.Clazz deviceClass;

    /**
     * storage directory where task result is uploaded
     */
    private ResultPath resultPath;


    public boolean equals(Object obj){

        if(!(obj instanceof TaskTrigger)){
            return false;
        }
        TaskTrigger tt = (TaskTrigger)obj;
        return null != tt.getId() && tt.getId().equals(this.id);
    }
}
