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

package ai.starwhale.mlops.schedule.impl.container.impl;

import ai.starwhale.mlops.common.DockerImage;
import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.spec.ContainerSpec;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import cn.hutool.json.JSONUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;


@Slf4j
public class RuntimeDockerizedContainerSpecification implements ContainerSpecification {

    final Task task;

    final String instanceUri;

    final SystemSettingService systemSettingService;
    final TaskTokenValidator taskTokenValidator;

    final ContainerSpec containerSpec;

    final StepSpec spec;

    final RuntimeVersionMapper runtimeVersionMapper;


    public RuntimeDockerizedContainerSpecification(
            Task task, String instanceUri,
            SystemSettingService systemSettingService,
            TaskTokenValidator taskTokenValidator,
            RuntimeVersionMapper runtimeVersionMapper
    ) {
        this.task = task;
        spec = task.getStep().getSpec();
        this.runtimeVersionMapper = runtimeVersionMapper;
        if (null == spec || null == spec.getContainerSpec()) {
            throw new SwValidationException(
                    ValidSubject.TASK,
                    "task is expected to have custom step spec when building the entrypoint"
            );
        }
        this.containerSpec = spec.getContainerSpec();
        this.instanceUri = instanceUri;
        this.systemSettingService = systemSettingService;
        this.taskTokenValidator = taskTokenValidator;
    }


    @Override
    public Map<String, String> getContainerEnvs() {
        Job job = task.getStep().getJob();
        var runTimeProperties = systemSettingService.getRunTimeProperties();
        Map<String, String> containerEnvs = new HashMap<>();
        containerEnvs.put("SW_INSTANCE_URI", instanceUri);
        containerEnvs.put("SW_PROJECT", job.getProject().getName());
        containerEnvs.put("SW_TOKEN", taskTokenValidator.getTaskToken(job.getOwner(), task.getId()));
        updateDockerSettingsEnv(containerEnvs);
        updatePypiSettingsEnv(runTimeProperties, containerEnvs);
        var taskEnv = task.getTaskRequest().getEnv();
        if (!CollectionUtils.isEmpty(taskEnv)) {
            taskEnv.forEach(env -> containerEnvs.put(env.getName(), env.getValue()));
        }
        return containerEnvs;
    }

    private void updatePypiSettingsEnv(RunTimeProperties runTimeProperties, Map<String, String> containerEnvs) {
        if (null == runTimeProperties.getPypi()) {
            return;
        }
        containerEnvs.put("SW_PYPI_INDEX_URL", runTimeProperties.getPypi().getIndexUrl());
        containerEnvs.put("SW_PYPI_EXTRA_INDEX_URL", runTimeProperties.getPypi().getExtraIndexUrl());
        containerEnvs.put("SW_PYPI_TRUSTED_HOST", runTimeProperties.getPypi().getTrustedHost());
        containerEnvs.put("SW_PYPI_TIMEOUT", String.valueOf(runTimeProperties.getPypi().getTimeout()));
        containerEnvs.put("SW_PYPI_RETRIES", String.valueOf(runTimeProperties.getPypi().getRetries()));
    }

    private void updateDockerSettingsEnv(Map<String, String> containerEnvs) {
        DockerSetting dockerSetting = systemSettingService.getDockerSetting();
        if (null == dockerSetting
                || null == dockerSetting.getRegistryForPush()
                || null == dockerSetting.getRegistryForPull()) {
            throw new SwProcessException(
                    ErrorType.SYSTEM,
                    "docker setting is required in your system setting example: "
                            + "dockerSetting:\n"
                            + "  registryForPull: \"docker-registry.starwhale.cn/star-whale\"\n"
                            + "  registryForPush: \"homepage-bj.intra.starwhale.ai/star-whale\"\n"
                            + "  userName: \"\"\n"
                            + "  password: \"\"\n"
                            + "  insecure: true"
            );
        }
        containerEnvs.put(
                "SW_CACHE_REPO",
                new DockerImage(
                        dockerSetting.getRegistryForPush(),
                        "cache"
                ).toString()
        );
        var dockerConfigJson = JSONUtil.toJsonStr(
                Map.of("auths", Map.of(dockerSetting.getRegistryForPush(), Map.of(
                        "username", dockerSetting.getUserName(),
                        "password", dockerSetting.getPassword()
                ))));
        containerEnvs.put("SW_DOCKER_REGISTRY_KEYS", dockerConfigJson);
        if (dockerSetting.isInsecure()) {
            containerEnvs.put("SW_DOCKER_REGISTRY_INSECURE", "true");
        }
    }

    @Override
    public ContainerCommand getCmd() {
        return new ContainerCommand(containerSpec.getCmds(), containerSpec.getEntrypoint());
    }

    @Override
    public String getImage() {
        return containerSpec.getImage();
    }

}
