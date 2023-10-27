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

import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.RunTimeProperties.Pypi;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


public class OnlineEvaluationContainerSpecification implements ContainerSpecification {

    final Task task;

    final TaskTokenValidator taskTokenValidator;

    final String instanceUri;

    final RunTimeProperties runTimeProperties;

    public OnlineEvaluationContainerSpecification(
            Task task,
            TaskTokenValidator taskTokenValidator,
            String instanceUri,
            RunTimeProperties runTimeProperties
    ) {
        this.task = task;
        this.taskTokenValidator = taskTokenValidator;
        this.instanceUri = instanceUri;
        this.runTimeProperties = runTimeProperties;
    }

    @Override
    public Map<String, String> getContainerEnvs() {
        Job job = task.getStep().getJob();
        var model = job.getModel();
        var runtime = job.getJobRuntime();
        var envs = new HashMap<String, String>();

        envs.put("SW_RUNTIME_PYTHON_VERSION", runtime.pythonVersion());
        envs.put("SW_VERSION", runtime.swVersion());
        envs.put("SW_RUNTIME_VERSION", String.format("%s/version/%s", runtime.getName(), runtime.getVersion()));
        envs.put("SW_RUNTIME_URI",
                String.format(
                        FORMATTER_URI_ARTIFACT,
                        instanceUri,
                        runtime.getProjectId(),
                        "runtime",
                        runtime.getName(),
                        runtime.getVersion()));
        envs.put(
                "SW_MODEL_VERSION",
                String.format("%s/version/%s", job.getModel().getName(), job.getModel().getVersion())
        );
        envs.put("SW_MODEL_URI",
                String.format(
                        FORMATTER_URI_ARTIFACT,
                        instanceUri,
                        model.getProjectId(),
                        "model",
                        model.getName(),
                        model.getVersion()));
        envs.put("SW_INSTANCE_URI", instanceUri);
        envs.put("SW_TOKEN", taskTokenValidator.getTaskToken(job.getOwner(), task.getId()));
        envs.put("SW_PROJECT", job.getProject().getName());
        envs.put("SW_PROJECT_URI", String.format(
                FORMATTER_URI_PROJECT,
                instanceUri,
                job.getProject().getId()));
        setPypiSettings(envs);

        envs.put("SW_PRODUCTION", "1");
        if (StringUtils.hasText(runTimeProperties.getCondarc())) {
            envs.put("SW_CONDA_CONFIG", runTimeProperties.getCondarc());
        }
        var taskEnv = task.getTaskRequest().getEnv();
        if (!CollectionUtils.isEmpty(taskEnv)) {
            taskEnv.forEach(env -> envs.put(env.getName(), env.getValue()));
        }
        return envs;
    }

    private void setPypiSettings(HashMap<String, String> envs) {
        Pypi pypiSettings = runTimeProperties.getPypi();
        if (null != pypiSettings) {
            envs.put("SW_PYPI_INDEX_URL", getValue(pypiSettings.getIndexUrl()));
            envs.put("SW_PYPI_EXTRA_INDEX_URL", getValue(pypiSettings.getExtraIndexUrl()));
            envs.put("SW_PYPI_TRUSTED_HOST", getValue(pypiSettings.getTrustedHost()));
            envs.put("SW_PYPI_TIMEOUT", String.valueOf(pypiSettings.getTimeout()));
            envs.put("SW_PYPI_RETRIES", String.valueOf(pypiSettings.getRetries()));
        }
    }

    private String getValue(String s) {
        return s == null ? "" : s;
    }

    @Override
    public ContainerCommand getCmd() {
        return ContainerCommand.builder().cmd(new String[] {"serve"}).build();
    }

    @Override
    public String getImage() {
        return task.getStep().getJob().getJobRuntime().getImage();
    }


}
