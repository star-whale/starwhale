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

package ai.starwhale.mlops.agent.task.inferencetask.action.normal;

import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceStage;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.domain.node.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class Init2PreparingAction extends AbsBaseTaskAction {

    @Override
    public InferenceTask processing(InferenceTask originTask, Context context) throws Exception {

        // try to allocate device for task, otherwise just wait to allocate next action(preparing2Running)
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
        originTask.setDevices(allocated);
        return originTask;
    }

    @Override
    public void success(InferenceTask originTask, InferenceTask newTask, Context context) {
        // add the new task to the tail
        taskPool.add2PreparingQueue(newTask);
    }

    @Override
    public InferenceStage stage() {
        return InferenceStage.INIT2PREPARING;
    }
}
