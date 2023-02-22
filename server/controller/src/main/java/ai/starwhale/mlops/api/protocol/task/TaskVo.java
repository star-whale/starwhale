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

package ai.starwhale.mlops.api.protocol.task;

import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@EqualsAndHashCode
@Schema(description = "Task object", title = "Task")
@Validated
public class TaskVo implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("createdTime")
    private Long createdTime;

    @JsonProperty("taskStatus")
    private TaskStatus taskStatus;

    @JsonProperty("retryNum")
    private Integer retryNum;

    @JsonProperty("resourcePool")
    private String resourcePool;

    public static TaskVo empty() {
        return new TaskVo("", "", -1L, TaskStatus.CREATED, 0, ResourcePool.DEFAULT_NAME);
    }
}
