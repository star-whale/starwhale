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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.domain.node.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRuntime {

    /**
     * specify the job to run on whether CPU or GPU
     */
    Device.Clazz deviceClass;

    /**
     * how many devices does this job need to run on ie. how many tasks shall be split from the job
     */
    Integer deviceAmount;

    /**
     * what is the running container's image
     */
    String baseImage;

    public JobRuntime copy(){
        return  new JobRuntime(this.deviceClass,this.deviceAmount,this.baseImage);
    }
}
