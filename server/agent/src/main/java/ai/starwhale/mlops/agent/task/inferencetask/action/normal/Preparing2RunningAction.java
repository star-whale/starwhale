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

import ai.starwhale.mlops.agent.container.ImageConfig;
import ai.starwhale.mlops.agent.container.ImageConfig.CPUConfig;
import ai.starwhale.mlops.agent.container.ImageConfig.GPUConfig;
import ai.starwhale.mlops.agent.container.ImageConfig.Mount;
import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.node.SourcePool.AllocateRequest;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask.ActionStatus;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.domain.node.Device;
import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class Preparing2RunningAction extends AbsBaseTaskAction {
    private static final String containerBasePath = "/opt/starwhale/";
    private static final String runtimeDepPath = "/opt/starwhale/swmp/dep";
    private static final String runtimeManifestFilePath = "/opt/starwhale/swmp/_manifest.yaml";
    private static final String swmpDirEnv = "SW_SWMP_WORKDIR";
    private static final String statusFileEnv = "SW_TASK_STATUS_FILE";
    private static final String logDirEnv = "SW_TASK_LOG_DIR";
    private static final String resultDirEnv = "SW_TASK_RESULT_DIR";
    private static final String swdsFileEnv = "SW_TASK_INPUT_CONFIG";

    private static final String pipCachePathFormat = "%s/.cache/pip";

    /*@Override
    public boolean valid(InferenceTask task, Context context) {
        // todo: Check if the previous steps have been prepared
        return task.getActionStatus() != ActionStatus.inProgress;
    }*/

    @Override
    public void orElse(InferenceTask task, Context context) {
        // represent last step occurred some errors
        // todo:fault tolerance
        task.setActionStatus(ActionStatus.completed);
        taskPersistence.save(task);
    }

    @Override
    public InferenceTask processing(InferenceTask originTask, Context context) throws Exception {

        ImageConfig imageConfig = ImageConfig.builder()
                .autoRemove(false) // finally rm
                // .image(originTask.getImageId())
                .labels(Map.of(
                        "task-id", originTask.getId().toString(),
                        "task-type", originTask.getTaskType().name(),
                        "swmp-name", originTask.getSwModelPackage().getName(),
                        "swmp-version", originTask.getSwModelPackage().getVersion(),
                        "device-type", originTask.getDeviceClass().name(),
                        "device-num", originTask.getDeviceAmount().toString()
                ))
                .build();

        // preAllocate fail, try again
        Set<Device> allocated = originTask.getDevices();
        if (CollectionUtil.isEmpty(allocated)) {
            
            // allocate device(GPU or CPU) for task
            switch (originTask.getDeviceClass()) {
                case CPU:
                    allocated = sourcePool.allocate(AllocateRequest.builder().cpuNum(originTask.getDeviceAmount()).build());
                    break;
                case GPU:
                    allocated = sourcePool.allocate(AllocateRequest.builder().gpuNum(originTask.getDeviceAmount()).build());
                    break;
                case UNKNOWN:
                    log.error("unknown device class");
                    throw ErrorCode.allocateError.asException("unknown device class");
            }
            
        }
        // config for container
        switch (originTask.getDeviceClass()) {
            case CPU:
                imageConfig.setCpuConfig(
                        CPUConfig.builder().cpuCount(Long.valueOf(originTask.getDeviceAmount())).build()
                );
                break;
            case GPU:
                imageConfig.setGpuConfig(
                        GPUConfig.builder()
                                // .count(originTask.getDeviceAmount())
                                .capabilities(List.of(List.of("gpu")))
                                .deviceIds(allocated.stream().map(Device::getId).collect(Collectors.toList()))
                                .build()
                );
                break;
        }

        switch (originTask.getTaskType()) {
            case PPL:
                imageConfig.setCmd(List.of("ppl"));
                break;
            case CMP:
                imageConfig.setCmd(List.of("cmp"));
                break;
        }
        taskPersistence.preloadingSWRT(originTask);
        String image = taskPersistence.runtimeManifest(originTask).getBaseImage();
        // use default image
        if (!StringUtils.hasText(image)) {
            image = agentProperties.getTask().getDefaultImage();
        }
        originTask.setImageId(image);
        // must be swrt file preloaded
        imageConfig.setImage(image);

        taskPersistence.preloadingSWMP(originTask);
        imageConfig.setMounts(List.of(
                Mount.builder()
                        .readOnly(false)
                        .source(fileSystemPath.oneActiveTaskDir(originTask.getId()))
                        .target(containerBasePath)
                        .type("BIND")
                        .build(),
                Mount.builder()
                        .readOnly(false)
                        .source(fileSystemPath.oneActiveTaskRuntimeDir(originTask.getId()) + "/dep")
                        .target(runtimeDepPath)
                        .type("BIND")
                        .build(),
                Mount.builder()
                        .readOnly(false)
                        .source(fileSystemPath.oneActiveTaskRuntimeManifestFile(originTask.getId()))
                        .target(runtimeManifestFilePath)
                        .type("BIND")
                        .build(),
                Mount.builder()
                        .readOnly(false)
                        .source(String.format(pipCachePathFormat, agentProperties.getBasePath()))
                        .target(String.format(pipCachePathFormat, "/root"))
                        .type("BIND")
                        .build()

        ));
        // generate the file used by container(default dir)
        taskPersistence.generateConfigFile(originTask);

        // task container env
        imageConfig.setEnv(List.of(
                env("SW_PIP_CACHE_DIR", String.format(pipCachePathFormat, "root")), // todo specified by user
                env("SW_PYPI_INDEX_URL", agentProperties.getTask().getPypiIndexUrl()),
                env("SW_PYPI_EXTRA_INDEX_URL", agentProperties.getTask().getPypiExtraIndexUrl()),
                env("SW_PYPI_TRUSTED_HOST", agentProperties.getTask().getPypiTrustedHost()),
                env("SW_SWMP_NAME", originTask.getSwModelPackage().getName()),
                env("SW_SWMP_VERSION", originTask.getSwModelPackage().getVersion())
        ));

        // fill with task info
        Optional<String> containerId = containerClient.createAndStartContainer(imageConfig);
        // whether the container create and start success
        if (containerId.isPresent()) {
            // allocate device to this task,if fail will throw exception, now it is blocked
            originTask.setDevices(allocated);
            originTask.setContainerId(containerId.get());
            originTask.setStatus(InferenceTaskStatus.RUNNING);
            return originTask;
        } else {
            // should throw exception and handled by the fail method
            throw ErrorCode.containerError.asException(
                    String.format("start task container by image:%s fail", originTask.getImageId()));
        }

    }

    private String env(String key, String value) {
        return String.format("%s=%s", key, value);
    }

    @Override
    public void success(InferenceTask originTask, InferenceTask newTask, Context context) {
        // rm from current
        taskPool.preparingTasks.remove(originTask);
        // tail it to the running list
        taskPool.runningTasks.add(newTask);
    }

    @Override
    public void fail(InferenceTask originTask, Context context, Exception e) {
        log.error("execute task:{}, error:{}", originTask.getId(), e.getMessage());
        
        if (originTask.getRetryRunNum() >= agentProperties.getTask().getRetryRunMaxNum()) {
            // release device and move to failed list
            log.error("task:{} maximum number of failed retries:{} has been reached, task failed",
                    originTask.getId(), agentProperties.getTask().getRetryRunMaxNum());
            sourcePool.release(originTask.getDevices());
            originTask.setStatus(InferenceTaskStatus.FAIL);
            taskPool.preparingTasks.remove(originTask);
            taskPool.failedTasks.add(originTask);
            taskPersistence.save(originTask);
        } else {
            // todo: retry or take it to the tail of queue
            originTask.retryRun();
            taskPersistence.save(originTask);
        }
    }
}
