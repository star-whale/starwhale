/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.normal;

import ai.starwhale.mlops.agent.container.ImageConfig;
import ai.starwhale.mlops.agent.exception.ContainerException;
import ai.starwhale.mlops.agent.node.SourcePool.AllocateRequest;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.EvaluationTask.Stage;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class Preparing2RunningAction extends AbsBaseTaskTransition {

    @Override
    public boolean valid(EvaluationTask task, Context context) {
        return task.getStage() != Stage.inProgress;
    }

    @Override
    public void orElse(EvaluationTask task, Context context) {
        // represent last time occurred some errors
        // todo:fault tolerance
    }

    @Override
    public EvaluationTask processing(EvaluationTask oldTask, Context context) {
        //
        // allocate device(GPU or todo CPU) for task
        Set<Device> allocated = sourcePool.allocate(
            AllocateRequest.builder().gpuNum(1).build());

        // allocate device to this task,if fail will throw exception, now it is blocked
        oldTask.setDevices(allocated);
        // todo fill with task info
        Optional<String> containerId = containerClient.startContainer("",
            ImageConfig.builder().build());
        // whether the container create and start success
        if (containerId.isPresent()) {
            EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
            newTask.setContainerId(containerId.get());
            newTask.getTask().setStatus(TaskStatus.RUNNING);
            return newTask;
        } else {
            // todo: retry or take it to the tail of queue
            // should release, throw exception and handled by the fail method
            throw new ContainerException(
                String.format("start task container by image:%s fail", ""));
        }

    }

    @Override
    public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
        // rm from current
        taskPool.preparingTasks.remove(oldTask);
        // tail it to the running list
        taskPool.runningTasks.add(newTask);
    }

    @Override
    public void fail(EvaluationTask oldTask, Context context, Exception e) {
        // rollback and wait again until next time
        if (CollectionUtil.isNotEmpty(oldTask.getDevices())) {
            sourcePool.release(oldTask.getDevices());
            oldTask.setDevices(null);
        }
    }
}
