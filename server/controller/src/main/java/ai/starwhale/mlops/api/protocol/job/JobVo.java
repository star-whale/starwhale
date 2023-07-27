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

import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import javax.validation.Valid;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Schema(description = "Job object", title = "Job")
public class JobVo implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("modelName")
    private String modelName;

    @JsonProperty("modelVersion")
    private String modelVersion;

    @JsonProperty("handler")
    private String handler;

    @JsonProperty("datasets")
    @Valid
    private List<String> datasets;

    @JsonProperty("runtime")
    private RuntimeVo runtime;

    @JsonProperty("isBuiltinRuntime")
    private Boolean builtinRuntime;

    @JsonProperty("device")
    private String device;

    @JsonProperty("deviceAmount")
    private Integer deviceAmount;

    @JsonProperty("owner")
    private UserVo owner;

    @JsonProperty("createdTime")
    private Long createdTime;

    @JsonProperty("stopTime")
    private Long stopTime;

    @JsonProperty("jobStatus")
    private JobStatus jobStatus;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("resourcePool")
    private String resourcePool;

    private Long duration;

    // expose links is used to get the serving url of the model, it may contain:
    // 1. vscode url when the model is running under dev mode
    // 2. serving url when the model is running a web handler (which using the non-zero expose handler decorator)
    private List<ExposedLinkVo> exposedLinks;

    @JsonProperty("duration")
    public Long getDuration() {
        if (null != duration) {
            return duration;
        }
        if (null == stopTime || stopTime <= 0) {
            return System.currentTimeMillis() - createdTime;
        }
        return stopTime - createdTime;
    }

    @JsonProperty("pinnedTime")
    private Long pinnedTime;

}
