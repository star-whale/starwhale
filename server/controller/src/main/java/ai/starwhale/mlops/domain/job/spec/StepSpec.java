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

package ai.starwhale.mlops.domain.job.spec;

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepSpec {

    /**
     * the name of job which would be executed at job yaml during running time
     */
    @JsonProperty("job_name")
    private String jobName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("show_name")
    private String showName;

    private Integer concurrency = 1;

    private List<String> needs;

    private List<RuntimeResource> resources;

    private List<Env> env;

    private Integer replicas = 1;

    // https://github.com/star-whale/starwhale/pull/2350
    private Integer expose;
    private Boolean virtual;

    @JsonProperty("need_dataset")
    private Boolean needDataset;
}


