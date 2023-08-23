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
import ai.starwhale.mlops.configuration.RunTimeProperties.RunConfig;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


public class DatasetBuildContainerSpecification implements ContainerSpecification {

    final SystemSettingService systemSettingService;
    final TaskTokenValidator taskTokenValidator;

    final String instanceUri;

    final Task task;

    public DatasetBuildContainerSpecification(SystemSettingService systemSettingService,
            @Value("${sw.instance-uri}") String instanceUri,
            TaskTokenValidator taskTokenValidator, Task task) {
        this.systemSettingService = systemSettingService;
        this.taskTokenValidator = taskTokenValidator;
        this.instanceUri = instanceUri;
        this.task = task;
    }

    @Override
    public Map<String, String> getContainerEnvs() {
        var runTimeProperties = systemSettingService.getRunTimeProperties();
        String swVersion = "";
        String pyVersion = "";
        if (null != runTimeProperties.getDatasetBuild()) {
            RunConfig dsBuildConfig = runTimeProperties.getDatasetBuild();
            if (runTimeProperties != null && dsBuildConfig != null) {
                if (StringUtils.hasText(dsBuildConfig.getClientVersion())) {
                    swVersion = dsBuildConfig.getClientVersion();
                }
                if (StringUtils.hasText(dsBuildConfig.getPythonVersion())) {
                    pyVersion = dsBuildConfig.getPythonVersion();
                }
            }
        }
        Job swJob = task.getStep().getJob();
        var taskEnv = task.getTaskRequest().getEnv();
        Map<String, String> coreContainerEnvs = new HashMap<>();
        if (!CollectionUtils.isEmpty(taskEnv)) {
            taskEnv.forEach(env -> coreContainerEnvs.put(env.getName(), env.getValue()));
        }
        coreContainerEnvs.put("SW_VERSION", swVersion);
        coreContainerEnvs.put("SW_RUNTIME_PYTHON_VERSION", pyVersion);
        if (null != runTimeProperties.getPypi()) {
            coreContainerEnvs.put("SW_PYPI_INDEX_URL", runTimeProperties.getPypi().getIndexUrl());
            coreContainerEnvs.put("SW_PYPI_EXTRA_INDEX_URL", runTimeProperties.getPypi().getExtraIndexUrl());
            coreContainerEnvs.put("SW_PYPI_TRUSTED_HOST", runTimeProperties.getPypi().getTrustedHost());
            coreContainerEnvs.put("SW_PYPI_TIMEOUT", String.valueOf(runTimeProperties.getPypi().getTimeout()));
            coreContainerEnvs.put("SW_PYPI_RETRIES", String.valueOf(runTimeProperties.getPypi().getRetries()));
        }
        coreContainerEnvs.put("SW_INSTANCE_URI", instanceUri);
        coreContainerEnvs.put("SW_PROJECT", swJob.getProject().getName());
        coreContainerEnvs.put("SW_TOKEN", taskTokenValidator.getTaskToken(swJob.getOwner(), task.getId()));
        return coreContainerEnvs;
    }

    @Override
    public ContainerCommand getCmd() {
        return ContainerCommand.builder().cmd(new String[]{"dataset_build"}).build();
    }

    @Override
    public String getImage() {
        var runTimeProperties = systemSettingService.getRunTimeProperties();
        DockerImage dockerImage = new DockerImage("docker-registry.starwhale.cn/star-whale/starwhale:latest");
        if (null != runTimeProperties.getDatasetBuild() && StringUtils.hasText(
                runTimeProperties.getDatasetBuild().getImage())) {
            dockerImage = new DockerImage(runTimeProperties.getDatasetBuild().getImage());
        }
        return dockerImage.resolve(systemSettingService.getDockerSetting().getRegistryForPull());
    }


}
