package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.api.protocol.task.TaskVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.RandomUtil;
import ai.starwhale.mlops.domain.task.bo.Task;
import com.github.pagehelper.PageHelper;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class TaskService {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private TaskConvertor taskConvertor;

    @Resource
    private TaskMapper taskMapper;


    public List<TaskVO> listTasks(String jobId, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<TaskEntity> tasks = taskMapper.listTasks(idConvertor.revert(jobId));

        return tasks.stream()
            .map(taskConvertor::convert)
            .collect(Collectors.toList());
    }

    public TaskVO findTask(Long taskId) {
        TaskEntity entity = taskMapper.findTaskById(taskId);

        return taskConvertor.convert(entity);
    }

    public Boolean addTask(Task task) {
        String uuid = task.getUuid();
        if(!StringUtils.hasText(uuid)) {
            uuid = RandomUtil.randomHexString(16);
            task.setUuid(uuid);
        }
        TaskEntity entity = TaskEntity.builder()
            .jobId(task.getJob().getId())
            .taskUuid(uuid)
            .taskStatus(task.getStatus().getValue())
            .build();
        return taskMapper.addTask(entity) > 0;
    }
}
