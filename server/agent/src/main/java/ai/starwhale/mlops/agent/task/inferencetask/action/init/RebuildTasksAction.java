/**
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

package ai.starwhale.mlops.agent.task.inferencetask.action.init;

import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.Action;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.task.inferencetask.TaskPool;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
import ai.starwhale.mlops.domain.node.Device;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RebuildTasksAction implements Action<Void, List<InferenceTask>> {

    @Autowired
    private TaskPool taskPool;

    @Autowired
    private SourcePool sourcePool;

    @Autowired
    private TaskPersistence taskPersistence;

    @Override
    public boolean valid(Void v, Context context) {
        return !taskPool.isReady();
    }

    @Override
    public List<InferenceTask> processing(Void v, Context context)
            throws Exception {
        log.info("start to rebuild task pool");
        List<InferenceTask> tasks = taskPersistence.getAllActiveTasks().orElse(List.of());
        tasks.forEach(taskPool::fill);
        return tasks;
    }

    @Override
    public void success(Void v, List<InferenceTask> tasks, Context context) {
        // ensure by commandline runner order
        if (sourcePool.isReady()) {
            var running = taskPool.runningTasks.stream().filter(task -> task.getStatus() == InferenceTaskStatus.RUNNING).collect(Collectors.toList());
            running.forEach(task -> {
                Set<Device> allocated = null;
                try {
                    // allocate device(GPU or CPU) for task
                    switch (task.getDeviceClass()) {
                        case CPU:
                            allocated = sourcePool.allocate(
                                    SourcePool.AllocateRequest.builder().cpuNum(task.getDeviceAmount()).build());
                            break;
                        case GPU:
                            allocated = sourcePool.allocate(
                                    SourcePool.AllocateRequest.builder().gpuNum(task.getDeviceAmount()).build());
                            break;
                        case UNKNOWN:
                            log.error("unknown device class");
                            throw ErrorCode.allocateError.asException("unknown device class");
                    }
                    task.setDevices(allocated);
                } catch (Exception e) {
                    log.error("init task:{} error:{}", JSONUtil.toJsonStr(task), e.getMessage());
                }

            });
        }
        taskPool.setToReady();
        log.info("rebuild task pool success, size:{}", tasks.size());
    }

    @Override
    public void fail(Void v, Context context, Exception e) {
        log.info("rebuild task pool error:{}", e.getMessage(), e);
    }
}
