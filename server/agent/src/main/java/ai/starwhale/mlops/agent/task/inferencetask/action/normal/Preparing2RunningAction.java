/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.action.normal;

import ai.starwhale.mlops.agent.container.ImageConfig;
import ai.starwhale.mlops.agent.container.ImageConfig.GPUConfig;
import ai.starwhale.mlops.agent.container.ImageConfig.Mount;
import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.node.SourcePool.AllocateRequest;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask.ActionStatus;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.task.TaskStatus;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Preparing2RunningAction extends AbsBasePPLTaskAction {
    private static final String containerBasePath = "/opt/starwhale/";
    private static final String swmpDirEnv = "SW_SWMP_WORKDIR";
    private static final String statusFileEnv = "SW_TASK_STATUS_FILE";
    private static final String logDirEnv = "SW_TASK_LOG_DIR";
    private static final String resultDirEnv = "SW_TASK_RESULT_DIR";
    private static final String swdsFileEnv = "SW_TASK_SWDS_CONFIG";

    @Override
    public boolean valid(InferenceTask task, Context context) {
        // todo: Check if the previous steps have been prepared
        return task.getActionStatus() != ActionStatus.inProgress;
    }

    @Override
    public void orElse(InferenceTask task, Context context) {
        // represent last step occurred some errors
        // todo:fault tolerance
        task.setActionStatus(ActionStatus.completed);
        taskPersistence.save(task);
    }

    @Override
    public InferenceTask processing(InferenceTask oldTask, Context context) throws Exception {

        ImageConfig imageConfig = ImageConfig.builder()
                .autoRemove(true)
                .image(oldTask.getImageId())
                .labels(Map.of("taskId", oldTask.getId().toString()))
                .build();

        Set<Device> allocated = null;
        // allocate device(GPU or CPU) for task
        switch (oldTask.getDeviceClass()) {
            case CPU:
                allocated = sourcePool.allocate(
                        AllocateRequest.builder().cpuNum(oldTask.getDeviceAmount()).build());
                imageConfig.setCpuConfig(
                        ImageConfig.CPUConfig.builder()
                                .cpuCount(Long.valueOf(oldTask.getDeviceAmount()))
                                .build()
                );
                break;
            case GPU:
                allocated = sourcePool.allocate(
                        AllocateRequest.builder().gpuNum(oldTask.getDeviceAmount()).build());
                imageConfig.setGpuConfig(
                        GPUConfig.builder()
                                .count(oldTask.getDeviceAmount())
                                .capabilities(List.of(List.of("gpu")))
                                .deviceIds(
                                        allocated.stream().map(Device::getId).collect(Collectors.toList()))
                                .build()
                );
                break;
            case UNKNOWN:
                log.error("unknown device class");
                throw ErrorCode.allocateError.asException("unknown device class");
        }
        // allocate device to this task,if fail will throw exception, now it is blocked
        oldTask.setDevices(allocated);

        switch (oldTask.getTaskType()) {
            case PPL:
                imageConfig.setEntrypoint(List.of("ppl"));
                break;
            case CMP:
                imageConfig.setEntrypoint(List.of("cmp"));
                break;
        }

        // pull swmp(tar) and uncompress it to the swmp dir
        String swmpDir = taskPersistence.preloadingSWMP(oldTask);

        imageConfig.setMounts(List.of(
                Mount.builder()
                        .readOnly(false)
                        .source(fileSystemPath.oneActiveTaskDir(oldTask.getId()))
                        .target(containerBasePath)
                        .type("BIND")
                        .build(),
                Mount.builder()
                        .readOnly(false)
                        .source(swmpDir)
                        .target(containerBasePath + "swmp")
                        .type("BIND")
                        .build()
        ));

        taskPersistence.generateConfigFile(oldTask);


        // fill with task info
        Optional<String> containerId = containerClient.startContainer(imageConfig);
        // whether the container create and start success
        if (containerId.isPresent()) {
            InferenceTask newTask = BeanUtil.toBean(oldTask, InferenceTask.class);
            newTask.setContainerId(containerId.get());
            newTask.setStatus(TaskStatus.RUNNING);
            return newTask;
        } else {
            // todo: retry or take it to the tail of queue
            // should release, throw exception and handled by the fail method
            throw ErrorCode.containerError.asException(
                    String.format("start task container by image:%s fail", ""));
        }

    }

    private String env(String key, String value) {
        return String.format("%s=%s", key, value);
    }

    @Override
    public void success(InferenceTask oldTask, InferenceTask newTask, Context context) {
        // rm from current
        taskPool.preparingTasks.remove(oldTask);
        // tail it to the running list
        taskPool.runningTasks.add(newTask);
    }

    @Override
    public void fail(InferenceTask oldTask, Context context, Exception e) {
        log.error("execute task:{}, error:{}", JSONUtil.toJsonStr(oldTask), e.getMessage());
        // rollback and wait again until next time
        if (CollectionUtil.isNotEmpty(oldTask.getDevices())) {
            sourcePool.release(oldTask.getDevices());
            oldTask.setDevices(null);
        }
        oldTask.setActionStatus(ActionStatus.completed);
    }
}
