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

package ai.starwhale.mlops.schedule.k8s;

import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.system.resourcepool.bo.Toleration;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForSchedule;
import ai.starwhale.mlops.domain.task.status.watchers.log.TaskLogK8sCollector;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.json.JSONUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectFieldSelector;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class K8sTaskScheduler implements SwTaskScheduler {

    final K8sClient k8sClient;

    final RunTimeProperties runTimeProperties;

    final TaskTokenValidator taskTokenValidator;

    final K8sJobTemplate k8sJobTemplate;

    final String instanceUri;
    final int devPort;
    final int datasetLoadBatchSize;
    final String restartPolicy;
    final int backoffLimit;
    final StorageAccessService storageAccessService;
    final ThreadPoolTaskScheduler taskScheduler;

    private final TaskLogK8sCollector taskLogK8sCollector;

    public K8sTaskScheduler(
            K8sClient k8sClient,
            TaskTokenValidator taskTokenValidator,
            RunTimeProperties runTimeProperties,
            K8sJobTemplate k8sJobTemplate,
            @Value("${sw.instance-uri}") String instanceUri,
            @Value("${sw.task.dev-port}") int devPort,
            @Value("${sw.dataset.load.batch-size}") int datasetLoadBatchSize,
            @Value("${sw.infra.k8s.job.restart-policy}") String restartPolicy,
            @Value("${sw.infra.k8s.job.backoff-limit}") Integer backoffLimit,
            StorageAccessService storageAccessService,
            TaskLogK8sCollector taskLogK8sCollector,
            ThreadPoolTaskScheduler taskScheduler
    ) {
        this.k8sClient = k8sClient;
        this.taskTokenValidator = taskTokenValidator;
        this.runTimeProperties = runTimeProperties;
        this.k8sJobTemplate = k8sJobTemplate;
        this.instanceUri = instanceUri;
        this.devPort = devPort;
        this.storageAccessService = storageAccessService;
        this.datasetLoadBatchSize = datasetLoadBatchSize;
        this.restartPolicy = restartPolicy;
        this.backoffLimit = backoffLimit;
        this.taskLogK8sCollector = taskLogK8sCollector;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void schedule(Collection<Task> tasks) {
        tasks.forEach(this::deployTaskToK8s);
    }

    @Override
    public void stop(Collection<Task> tasks) {
        tasks.forEach(task -> {
            try {
                // K8s do not support job suspend before 1.24, so we collect logs and delete job directly
                // https://kubernetes.io/docs/concepts/workloads/controllers/job/#suspending-a-job
                taskLogK8sCollector.collect(task);
                k8sClient.deleteJob(task.getId().toString());
            } catch (ApiException e) {
                log.warn("delete k8s job failed {}, {}", task.getId(), e.getResponseBody(), e);
            }
        });
    }

    @Override
    public Future<String[]> exec(Task task, String... command) {
        try {
            var pods = k8sClient.getPodsByJobName(task.getId().toString());
            if (CollectionUtils.isEmpty(pods.getItems())) {
                throw new SwProcessException(ErrorType.K8S, "no pod found for task " + task.getId());
            }
            if (pods.getItems().size() != 1) {
                throw new SwProcessException(ErrorType.K8S, "multiple pods found for task " + task.getId());
            }
            return taskScheduler.submit(
                    () -> k8sClient.execInPod(pods.getItems().get(0).getMetadata().getName(), null, command));
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.K8S, "exec command failed: " + e.getResponseBody(), e);
        }
    }

    /**
     * {instance}/project/{projectName}/dataset/{datasetName}/version/{version}
     */
    static final String FORMATTER_URI_ARTIFACT = "%s/project/%s/%s/%s/version/%s";

    static final String FORMATTER_VERSION_ARTIFACT = "%s/version/%s";

    static final String ANNOTATION_KEY_JOB_ID = "starwhale.ai/job-id";
    static final String ANNOTATION_KEY_TASK_ID = "starwhale.ai/task-id";
    static final String ANNOTATION_KEY_USER_ID = "starwhale.ai/user-id";
    static final String ANNOTATION_KEY_PROJECT_ID = "starwhale.ai/project-id";

    private void deployTaskToK8s(Task task) {
        log.debug("deploying task to k8s {} ", task.getId());
        try {
            V1Job k8sJob = k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL);

            // TODO: use task's resource needs
            Map<String, ContainerOverwriteSpec> containerSpecMap = new HashMap<>();

            var job = task.getStep().getJob();
            JobRuntime jobRuntime = job.getJobRuntime();

            k8sJobTemplate.getContainersTemplates(k8sJob).forEach(templateContainer -> {
                ContainerOverwriteSpec containerOverwriteSpec = new ContainerOverwriteSpec(templateContainer.getName());
                containerOverwriteSpec.setEnvs(buildCoreContainerEnvs(task));
                containerOverwriteSpec.setCmds(List.of("run"));
                containerOverwriteSpec.setResourceOverwriteSpec(getResourceSpec(task));
                containerOverwriteSpec.setImage(jobRuntime.getImage());
                containerSpecMap.put(templateContainer.getName(), containerOverwriteSpec);
            });

            var pool = task.getStep().getResourcePool();
            if (pool == null) {
                // backward compatibility
                pool = task.getStep().getJob().getResourcePool();
            }
            Map<String, String> nodeSelector = pool != null ? pool.getNodeSelector() : Map.of();
            List<Toleration> tolerations = pool != null ? pool.getTolerations() : null;
            Map<String, String> annotations = new HashMap<>();

            var userId = job.getOwner() == null ? "" : job.getOwner().getId().toString();
            annotations.put(ANNOTATION_KEY_JOB_ID, job.getId().toString());
            annotations.put(ANNOTATION_KEY_TASK_ID, task.getId().toString());
            annotations.put(ANNOTATION_KEY_USER_ID, userId);
            annotations.put(ANNOTATION_KEY_PROJECT_ID, job.getProject().getId().toString());
            if (pool != null && !CollectionUtils.isEmpty(pool.getMetadata())) {
                annotations.putAll(pool.getMetadata());
            }

            k8sJobTemplate.renderJob(
                    k8sJob,
                    task.getId().toString(),
                    this.restartPolicy,
                    this.backoffLimit,
                    containerSpecMap,
                    nodeSelector,
                    tolerations,
                    annotations
            );

            // clear the args and activeDeadlineSeconds and make tail -f cmd if debug mode is on
            // this makes the job running forever
            if (job.isDevMode()) {
                log.info("dev mode of job {} is on, will not run the job, just tail -f /dev/null", job.getId());
                var workerContainer = k8sJob.getSpec().getTemplate().getSpec().getContainers().get(0);
                workerContainer.setArgs(List.of("dev"));
                k8sJob.getSpec().setActiveDeadlineSeconds(null);
            }

            log.debug("deploying k8sJob to k8s :{}", JSONUtil.toJsonStr(k8sJob));
            try {
                k8sClient.deleteJob(task.getId().toString());
                log.info("existing k8s job {} deleted  before start it", task.getId());
            } catch (ApiException e) {
                log.debug("try to delete existing k8s job {} before start it, however it doesn't exist", task.getId());
            }
            k8sClient.deployJob(k8sJob);
        } catch (ApiException k8sE) {
            log.error(" schedule task failed {}", k8sE.getResponseBody(), k8sE);
            taskFailed(task);
        } catch (Exception e) {
            log.error(" schedule task failed ", e);
            taskFailed(task);
        }
    }

    private ResourceOverwriteSpec getResourceSpec(Task task) {
        var runtimeResources = getPatchedResources(task);
        if (!CollectionUtils.isEmpty(runtimeResources)) {
            return new ResourceOverwriteSpec(runtimeResources);
        }
        return null;
    }

    private List<RuntimeResource> getPatchedResources(Task task) {
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

    @NotNull
    private List<V1EnvVar> buildCoreContainerEnvs(Task task) {
        Job swJob = task.getStep().getJob();
        var project = swJob.getProject();
        var model = swJob.getModel();
        var runtime = swJob.getJobRuntime();
        Map<String, String> coreContainerEnvs = new HashMap<>();
        var taskEnv = task.getTaskRequest().getEnv();
        if (!CollectionUtils.isEmpty(taskEnv)) {
            taskEnv.forEach(env -> coreContainerEnvs.put(env.getName(), env.getValue()));
        }
        coreContainerEnvs.put("SW_TASK_STEP", task.getStep().getName());
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
        var resources = getPatchedResources(task).stream();
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

        var envs = mapToEnv(coreContainerEnvs);

        envs.add(
                new V1EnvVar()
                        .name("SW_POD_NAME")
                        .valueFrom(
                                new V1EnvVarSource().fieldRef(
                                        new V1ObjectFieldSelector().fieldPath("metadata.name")))
        );
        return envs;
    }

    @NotNull
    private List<V1EnvVar> mapToEnv(Map<String, String> initContainerEnvs) {
        return initContainerEnvs.entrySet().stream()
                .map(entry -> new V1EnvVar().name(entry.getKey()).value(entry.getValue()))
                .collect(Collectors.toList());
    }

    private void taskFailed(Task task) {
        TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(Set.of(TaskWatcherForSchedule.class));
        // todo save log
        task.updateStatus(TaskStatus.FAIL);
        TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
    }

}
