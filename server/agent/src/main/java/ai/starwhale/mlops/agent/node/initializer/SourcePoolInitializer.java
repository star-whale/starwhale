/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.initializer;

import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.inferencetask.TaskPool;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.task.TaskStatus;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * execute on every startup
 */
@Slf4j
@Order(1)
public class SourcePoolInitializer implements CommandLineRunner {
    @Autowired
    private SourcePool sourcePool;

    @Autowired
    private TaskPool taskPool;

    @Override
    public void run(String... args) throws Exception {
        sourcePool.refresh();
        // ensure by order todo
        if (taskPool.isReady()) {
            var running = taskPool.runningTasks.stream().filter(task -> task.getStatus() == TaskStatus.RUNNING).collect(Collectors.toList());
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
        sourcePool.setToReady();
    }
}
