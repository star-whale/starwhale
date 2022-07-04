package ai.starwhale.mlops.agent.task.log;

import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
public class FileLog implements Appender, Reader {
    private final TaskPersistence taskPersistence;

    public FileLog(TaskPersistence taskPersistence) {
        this.taskPersistence = taskPersistence;
    }

    @Override
    public void append(InferenceTask task, String content) {
        taskPersistence.recordLog(task, content);
    }

    @Override
    public void finishAppend(InferenceTask task) {
        try {
            taskPersistence.uploadLog(task);
        } catch (Exception e) {
            log.error("upload log for task {} error", task.getId(), e);
        }
    }

    @Override
    public void read(String readerId, InferenceTask task) {

    }
}
