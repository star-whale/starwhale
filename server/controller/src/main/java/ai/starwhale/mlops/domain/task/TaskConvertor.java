package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.api.protocol.task.TaskVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.system.AgentConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class TaskConvertor implements Convertor<TaskEntity, TaskVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Resource
    private AgentConvertor agentConvertor;

    @Override
    public TaskVO convert(TaskEntity entity) throws ConvertException {
        if(entity == null) {
            return TaskVO.empty();
        }
        return TaskVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .uuid(entity.getTaskUuid())
            .taskStatus(entity.getTaskStatus())
            .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
            .agent(agentConvertor.convert(entity.getAgent()))
            .build();
    }

    @Override
    public TaskEntity revert(TaskVO vo) throws ConvertException {
        Objects.requireNonNull(vo, "TaskVO");
        return TaskEntity.builder()
            .taskStatus(vo.getTaskStatus())
            .taskUuid(vo.getUuid())
            .build();
    }
}
