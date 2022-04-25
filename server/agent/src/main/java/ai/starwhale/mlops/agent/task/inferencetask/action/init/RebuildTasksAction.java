/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
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
