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

package ai.starwhale.mlops.api.protocol.evaluation;

import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "Evaluation Summary object", title = "Evaluation")
@Validated
public class SummaryVo {

    @JsonProperty("id")
    private String id;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("projectId")
    private String projectId;

    @JsonProperty("projectName")
    private String projectName;

    @JsonProperty("modelName")
    private String modelName;

    @JsonProperty("modelVersion")
    private String modelVersion;

    @JsonProperty("datasets")
    private String datasets;

    @JsonProperty("runtime")
    private String runtime;

    @JsonProperty("device")
    private String device;

    @JsonProperty("deviceAmount")
    private Integer deviceAmount;

    @JsonProperty("createdTime")
    private Long createdTime;

    @JsonProperty("stopTime")
    private Long stopTime;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("duration")
    public Long duration;

    @JsonProperty("jobStatus")
    private JobStatus jobStatus;

    @JsonProperty("attributes")
    private List<AttributeValueVo> attributes;
}
