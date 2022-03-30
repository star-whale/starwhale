package ai.starwhale.mlops.domain.task;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TaskMapper {

    List<TaskEntity> listTasks(@Param("jobId")Long jobId);

    TaskEntity findTaskById(@Param("taskId") Long taskId);

    int addTask(TaskEntity taskEntity);
}
