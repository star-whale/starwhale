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

package ai.starwhale.mlops.domain.job.step.task;

import ai.starwhale.mlops.api.protocol.task.TaskVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.util.BatchOperateHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.task.bo.ResultPath;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.converter.TaskBoConverter;
import ai.starwhale.mlops.domain.job.step.task.converter.TaskConverter;
import ai.starwhale.mlops.domain.job.step.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.job.step.task.po.TaskEntity;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskWatcherForPersist;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class TaskService {

    private final TaskConverter taskConvertor;
    private final TaskBoConverter taskBoConvertor;

    private final TaskMapper taskMapper;

    private final StorageAccessService storageAccessService;

    private final IdConverter idConverter = new IdConverter();

    public TaskService(TaskConverter taskConvertor, TaskBoConverter taskBoConvertor, TaskMapper taskMapper,
                       StorageAccessService storageAccessService) {
        this.taskConvertor = taskConvertor;
        this.taskBoConvertor = taskBoConvertor;
        this.taskMapper = taskMapper;
        this.storageAccessService = storageAccessService;
    }

    public List<TaskVo> listTasks(Long jobId) {
        return taskMapper.listTasks(jobId).stream().map(taskConvertor::convert).collect(Collectors.toList());
    }

    public TaskVo getTask(String taskUrl) {
        if (idConverter.isId(taskUrl)) {
            return taskConvertor.convert(taskMapper.findTaskById(idConverter.revert(taskUrl)));
        } else {
            return taskConvertor.convert(taskMapper.findTaskByUuid(taskUrl));
        }
    }

    public void fillStepTasks(Step step) {
        var taskEntities = taskMapper.findByStepId(step.getId());
        var tasks = taskBoConvertor.fromTaskEntity(taskEntities, step);
        step.setTasks(tasks);
    }

    public List<String> offLineLogFiles(Long taskId) {
        ResultPath resultPath = resultPathOfTask(taskId);
        try {
            String logDir = resultPath.logDir();
            return storageAccessService.list(logDir)
                    .map(path -> trimPath(path, logDir))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.DB,
                    MessageFormat.format("read log path from db failed {0}", taskId),
                    e);
        }

    }

    public String logContent(Long taskId, String logFileName) {
        ResultPath resultPath = resultPathOfTask(taskId);
        String logDir = resultPath.logDir();
        try (InputStream inputStream = storageAccessService.get(
                logDir + PATH_SPLITERATOR + logFileName)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.DB,
                    MessageFormat.format("read log path from db failed {}", taskId),
                    e);
        }

    }

    /**
     * prevent send packet greater than @@GLOBAL.max_allowed_packet
     */
    static final Integer MAX_BATCH_SIZE = 500;

    public void batchInsertTasks(Collection<TaskEntity> entities) {
        BatchOperateHelper.doBatch(entities,
                ts -> taskMapper.addAll(ts.parallelStream().collect(Collectors.toList())), MAX_BATCH_SIZE);
    }

    public void batchUpdateTaskStatus(Collection<Task> tasks, TaskStatus taskStatus) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        //save to db
        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
        BatchOperateHelper.doBatch(
                taskIds,
                taskStatus,
                (batches, status) -> taskMapper.updateTaskStatus(new ArrayList<>(batches), status),
                MAX_BATCH_SIZE);

        CompletableFuture.runAsync(() -> {
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(Set.of(TaskWatcherForPersist.class));
            tasks.forEach(task -> task.updateStatus(taskStatus));
            TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
        });
    }

    private ResultPath resultPathOfTask(Long taskId) {
        TaskEntity taskById = taskMapper.findTaskById(taskId);
        return new ResultPath(taskById.getOutputPath());
    }

    static final String PATH_SPLITERATOR = "/";

    String trimPath(String fullPath, String dir) {
        return fullPath.replace(dir, "").replace(PATH_SPLITERATOR, "");
    }

}
