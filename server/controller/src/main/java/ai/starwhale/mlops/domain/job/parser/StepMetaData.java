/*
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

package ai.starwhale.mlops.domain.job.parser;

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.Data;

@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StepMetaData {

    /**
     * the name of job which would be executed at job yaml during running time
     */
    @JsonProperty("job_name")
    private String jobName;
    @JsonProperty("step_name")
    private String stepName;
    private Integer concurrency = 1;
    private List<String> needs;
    @JsonDeserialize(contentConverter = ResourceConverter.class)
    private List<RuntimeResource> resources;
    @JsonProperty("task_num")
    private Integer taskNum = 1;
}


