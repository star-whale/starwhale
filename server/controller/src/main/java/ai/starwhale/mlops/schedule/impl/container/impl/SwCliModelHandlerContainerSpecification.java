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

import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceOverwriteSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
public class SwCliModelHandlerContainerSpecification implements ContainerSpecification {


    static final String FORMATTER_URI_ARTIFACT = "%s/project/%s/%s/%s/version/%s";
    static final String FORMATTER_VERSION_ARTIFACT = "%s/version/%s";
    final String instanceUri;
    final int devPort;
    final int datasetLoadBatchSize;
    final RunTimeProperties runTimeProperties;
    final TaskTokenValidator taskTokenValidator;

    final Task task;

    public SwCliModelHandlerContainerSpecification(
            @Value("${sw.instance-uri}") String instanceUri,
            @Value("${sw.task.dev-port}") int devPort,
            @Value("${sw.dataset.load.batch-size}") int datasetLoadBatchSize,
            RunTimeProperties runTimeProperties,
            TaskTokenValidator taskTokenValidator,
            Task task
    ) {
        this.instanceUri = instanceUri;
        this.devPort = devPort;
        this.datasetLoadBatchSize = datasetLoadBatchSize;
        this.runTimeProperties = runTimeProperties;
        this.taskTokenValidator = taskTokenValidator;
        this.task = task;
    }


    public Map<String, String> getContainerEnvs() {
        Job swJob = task.getStep().getJob();
        var model = swJob.getModel();
        var runtime = swJob.getJobRuntime();
        Map<String, String> coreContainerEnvs = new HashMap<>();
        var taskEnv = task.getTaskRequest().getEnv();
        if (!CollectionUtils.isEmpty(taskEnv)) {
            taskEnv.forEach(env -> coreContainerEnvs.put(env.getName(), env.getValue()));
        }
        coreContainerEnvs.put("SW_RUNTIME_PYTHON_VERSION", runtime.getManifest().getEnvironment().getPython());
        coreContainerEnvs.put("SW_VERSION", runtime.getManifest().getEnvironment().getLock().getSwVersion());
        coreContainerEnvs.put("SW_TASK_STEP", task.getStep().getName());
        try {
            var stepSpecs = Constants.yamlMapper.readValue(swJob.getStepSpec(), new TypeReference<List<StepSpec>>() {
            });
            for (var stepSpec : stepSpecs) {
                if (task.getStep().getName().equals(stepSpec.getName())) {
                    if (StringUtils.hasText(stepSpec.getExtraCmdArgs())) {
                        coreContainerEnvs.put("SW_TASK_EXTRA_CMD_ARGS", stepSpec.getExtraCmdArgs());
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("parsing job step spec failed, is there any version conflict?", e);
            throw new SwProcessException(ErrorType.SYSTEM, "parsing job step spec failed", e);
        }
        coreContainerEnvs.put("DATASET_CONSUMPTION_BATCH_SIZE", String.valueOf(datasetLoadBatchSize));
        // support multi dataset uris
        coreContainerEnvs.put("SW_DATASET_URI",
                swJob.getDataSets().stream()
                        .map(dataSet -> String.format(
                                FORMATTER_URI_ARTIFACT,
                                instanceUri,
                                dataSet.getProjectId(),
                                "dataset",
                                dataSet.getName(),
                                dataSet.getVersion())
                        ).collect(Collectors.joining(" ")));
        coreContainerEnvs.put("SW_MODEL_URI",
                String.format(
                        FORMATTER_URI_ARTIFACT,
                        instanceUri,
                        model.getProjectId(),
                        "model",
                        model.getName(),
                        model.getVersion()));
        coreContainerEnvs.put("SW_RUNTIME_URI",
                String.format(
                        FORMATTER_URI_ARTIFACT,
                        instanceUri,
                        runtime.getProjectId(),
                        "runtime",
                        runtime.getName(),
                        runtime.getVersion()));
        coreContainerEnvs.put("SW_MODEL_VERSION",
                String.format(FORMATTER_VERSION_ARTIFACT,
                        model.getName(), model.getVersion()));
        coreContainerEnvs.put("SW_RUNTIME_VERSION",
                String.format(FORMATTER_VERSION_ARTIFACT,
                        runtime.getName(), runtime.getVersion()));
        coreContainerEnvs.put("SW_RUN_HANDLER", task.getTaskRequest().getJobName());
        coreContainerEnvs.put("SW_TASK_INDEX", String.valueOf(task.getTaskRequest().getIndex()));
        coreContainerEnvs.put("SW_TASK_NUM", String.valueOf(task.getTaskRequest().getTotal()));
        coreContainerEnvs.put("SW_JOB_VERSION", swJob.getUuid());

        // datastore env
        coreContainerEnvs.put("SW_TOKEN", taskTokenValidator.getTaskToken(swJob.getOwner(), task.getId()));
        coreContainerEnvs.put("SW_INSTANCE_URI", instanceUri);
        coreContainerEnvs.put("SW_PROJECT", swJob.getProject().getName());
        coreContainerEnvs.put("SW_PYPI_INDEX_URL", runTimeProperties.getPypi().getIndexUrl());
        coreContainerEnvs.put("SW_PYPI_EXTRA_INDEX_URL", runTimeProperties.getPypi().getExtraIndexUrl());
        coreContainerEnvs.put("SW_PYPI_TRUSTED_HOST", runTimeProperties.getPypi().getTrustedHost());
        coreContainerEnvs.put("SW_PYPI_TIMEOUT", String.valueOf(runTimeProperties.getPypi().getTimeout()));
        coreContainerEnvs.put("SW_PYPI_RETRIES", String.valueOf(runTimeProperties.getPypi().getRetries()));
        if (StringUtils.hasText(runTimeProperties.getCondarc())) {
            coreContainerEnvs.put("SW_CONDA_CONFIG", runTimeProperties.getCondarc());
        }

        // GPU resource
        var resources = deviceResourceRequirements(task).stream();
        //TODO remove ResourceOverwriteSpec.RESOURCE_GPU dependency on k8s impl
        var gpu = resources.anyMatch(r -> r.getType().equals(ResourceOverwriteSpec.RESOURCE_GPU) && r.getRequest() > 0);
        // overwrite visible devices to none
        if (!gpu) {
            // https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/user-guide.html#gpu-enumeration
            coreContainerEnvs.put("NVIDIA_VISIBLE_DEVICES", "");
        }

        if (swJob.isDevMode()) {
            coreContainerEnvs.put("SW_DEV_TOKEN", swJob.getDevPassword());
            coreContainerEnvs.put("SW_DEV_PORT", String.valueOf(devPort));
        }

        return coreContainerEnvs;
    }

    @Override
    public ContainerCommand getCmd() {
        return ContainerCommand.builder().cmd(new String[]{"run"}).build();
    }

    @Override
    public String getImage() {
        return task.getStep().getJob().getJobRuntime().getImage();
    }


    private List<RuntimeResource> deviceResourceRequirements(Task task) {
        List<RuntimeResource> runtimeResources = task.getTaskRequest().getRuntimeResources();
        var pool = task.getStep().getResourcePool();
        if (pool == null) {
            // use resource pool of job, for backward compatibility
            pool = task.getStep().getJob().getResourcePool();
        }
        if (pool != null) {
            runtimeResources = pool.patchResources(runtimeResources);
        }
        return runtimeResources;
    }
}
