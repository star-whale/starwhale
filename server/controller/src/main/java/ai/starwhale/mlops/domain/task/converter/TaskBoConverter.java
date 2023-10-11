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

package ai.starwhale.mlops.domain.task.converter;

import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.run.RunDao;
import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.domain.task.bo.ResultPath;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import cn.hutool.json.JSONUtil;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * convert task objects
 */
@Slf4j
@Service
public class TaskBoConverter {

    final RunDao runDao;

    public TaskBoConverter(RunDao runDao) {
        this.runDao = runDao;
    }


    public List<Task> fromTaskEntity(List<TaskEntity> entities, Step step) {
        return entities.parallelStream().map(entity -> transformTask(step, entity)).collect(Collectors.toList());
    }

    public Task transformTask(Step step, TaskEntity entity) {
        Task task = Task.builder()
                .id(entity.getId())
                .step(step)
                .status(entity.getTaskStatus())
                .retryNum(entity.getRetryNum())
                .uuid(entity.getTaskUuid())
                .resultRootPath(new ResultPath(entity.getOutputPath()))
                .taskRequest(JSONUtil.toBean(entity.getTaskRequest(), TaskRequest.class))
                .ip(entity.getIp())
                .currentRun(currentRun(entity.getId()))
                .devWay(entity.getDevWay())
                .build();
        Long start = entity.getStartedTime() == null ? null : entity.getStartedTime().getTime();
        Long finish = entity.getFinishedTime() == null ? null : entity.getFinishedTime().getTime();
        task.setStartTime(start);
        task.setFinishTime(finish);
        return task;
    }

    private Run currentRun(Long taskId) {
        List<Run> runs = runDao.findByTaskId(taskId);
        if (CollectionUtils.isEmpty(runs)) {
            return null;
        }
        for (var r : runs) {
            if (r.getStatus() == RunStatus.RUNNING || r.getStatus() == RunStatus.PENDING) {
                return r;
            }
        }
        return null;
    }

}
