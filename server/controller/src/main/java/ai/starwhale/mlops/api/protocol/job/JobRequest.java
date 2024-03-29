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

package ai.starwhale.mlops.api.protocol.job;

import ai.starwhale.mlops.domain.job.BizType;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.JobType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class JobRequest implements Serializable {

    @Deprecated
    @JsonProperty("modelVersionUrl")
    private String modelVersionUrl;

    @Deprecated
    @JsonProperty("datasetVersionUrls")
    private String datasetVersionUrls;

    @Deprecated
    @JsonProperty("runtimeVersionUrl")
    private String runtimeVersionUrl;

    private String modelVersionId;
    private String runtimeVersionId;

    private List<String> datasetVersionIds;
    private List<String> validationDatasetVersionIds;

    @JsonProperty("comment")
    private String comment;

    @NotNull
    @JsonProperty("resourcePool")
    private String resourcePool;

    @JsonProperty("handler")
    private String handler;

    @JsonProperty("stepSpecOverWrites")
    private String stepSpecOverWrites;

    @JsonProperty("bizType")
    private BizType bizType;

    @JsonProperty("bizId")
    private String bizId;

    @JsonProperty("type")
    private JobType type = JobType.EVALUATION;

    // jobs will not auto start if devMode is true
    @JsonProperty("devMode")
    private boolean devMode = false;

    @JsonProperty("devPassword")
    private String devPassword;

    @JsonProperty("devWay")
    private DevWay devWay = DevWay.VS_CODE;

    private Long timeToLiveInSec;

}
