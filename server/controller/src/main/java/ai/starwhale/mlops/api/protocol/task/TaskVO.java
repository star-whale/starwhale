/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api.protocol.task;

import ai.starwhale.mlops.api.protocol.agent.AgentVO;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "Task object", title = "Task")
@Validated
public class TaskVO implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("agent")
    private AgentVO agent;

    @JsonProperty("createdTime")
    private Long createdTime;

    @JsonProperty("taskStatus")
    private TaskStatus taskStatus;

    public static TaskVO empty() {
        return new TaskVO("", "", AgentVO.empty(), -1L, TaskStatus.CREATED);
    }
}
