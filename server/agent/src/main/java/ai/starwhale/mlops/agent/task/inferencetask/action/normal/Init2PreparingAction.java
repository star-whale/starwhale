/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.action.normal;

import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.domain.node.Device;
import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class Init2PreparingAction extends AbsBasePPLTaskAction {

    @Override
    public InferenceTask processing(InferenceTask originTask, Context context) throws Exception {
        InferenceTask newTask = BeanUtil.toBean(originTask, InferenceTask.class);
        // todo try to allocate device for task, otherwise just wait to allocate
        Set<Device> allocated = null;
        // allocate device(GPU or CPU) for task
        switch (originTask.getDeviceClass()) {
            case CPU:
                allocated = sourcePool.preAllocateWithoutThrow(SourcePool.AllocateRequest.builder().cpuNum(originTask.getDeviceAmount()).build());
                break;
            case GPU:
                allocated = sourcePool.preAllocateWithoutThrow(SourcePool.AllocateRequest.builder().gpuNum(originTask.getDeviceAmount()).build());
                break;
            case UNKNOWN:
                log.error("unknown device class");
                throw ErrorCode.allocateError.asException("unknown device class");
        }
        newTask.setDevices(allocated);
        return newTask;
    }

    @Override
    public void success(InferenceTask oldTask, InferenceTask newTask, Context context) {
        // add the new task to the tail
        taskPool.preparingTasks.offer(newTask);
    }
}
