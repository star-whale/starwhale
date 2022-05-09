/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.api.protocol.report.resp;

import ai.starwhale.mlops.domain.node.Device;
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
    private List<SWDSBlockVO> swdsBlocks;

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
