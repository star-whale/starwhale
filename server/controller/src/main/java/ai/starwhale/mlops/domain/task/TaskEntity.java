package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.domain.system.AgentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity extends BaseEntity {

    private Long id;

    private String taskUuid;

    private Long jobId;

    private Long agentId;

    private AgentEntity agent;

    private Integer taskStatus;

    private String resultPath;

    private String taskRequest;

    private Integer taskType;

}
