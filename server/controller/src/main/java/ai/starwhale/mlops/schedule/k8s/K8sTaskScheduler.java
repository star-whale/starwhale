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
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.json.JSONUtil;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ObjectFieldSelector;
import io.kubernetes.client.openapi.models.V1Pod;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class K8sTaskScheduler implements SwTaskScheduler {

    final K8sClient k8sClient;

    final RunTimeProperties runTimeProperties;

    final TaskTokenValidator taskTokenValidator;

    final K8sJobTemplate k8sJobTemplate;

    final String instanceUri;
    final int datasetLoadBatchSize;
    final String restartPolicy;
    final int backoffLimit;
    final StorageAccessService storageAccessService;

    public K8sTaskScheduler(K8sClient k8sClient,
            TaskTokenValidator taskTokenValidator,
            RunTimeProperties runTimeProperties,
            K8sJobTemplate k8sJobTemplate,
            ResourceEventHandler<V1Job> eventHandlerJob,
            ResourceEventHandler<V1Node> eventHandlerNode,
            ResourceEventHandler<V1Pod> eventHandlerPod, @Value("${sw.instance-uri}") String instanceUri,
            @Value("${sw.dataset.load.batch-size}") int datasetLoadBatchSize,
            @Value("${sw.infra.k8s.job.restart-policy}") String restartPolicy,
            @Value("${sw.infra.k8s.job.backoff-limit}") Integer backoffLimit,
            StorageAccessService storageAccessService) {
        this.k8sClient = k8sClient;
        this.taskTokenValidator = taskTokenValidator;
        this.runTimeProperties = runTimeProperties;
        this.k8sJobTemplate = k8sJobTemplate;
        this.instanceUri = instanceUri;
        this.storageAccessService = storageAccessService;
        this.datasetLoadBatchSize = datasetLoadBatchSize;
        this.restartPolicy = restartPolicy;
        this.backoffLimit = backoffLimit;
    }

    @Override
    public void schedule(Collection<Task> tasks) {
        tasks.forEach(this::deployTaskToK8s);
    }

    @Override
    public void stopSchedule(Collection<Long> taskIds) {
        taskIds.forEach(id -> {
            try {
                k8sClient.deleteJob(id.toString());
            } catch (ApiException e) {
                log.warn("delete k8s job failed {}", id, e);
            }
        });
    }

    /**
     * {instance}/project/{projectName}/dataset/{datasetName}/version/{version}
     */
    static final String FORMATTER_URI_DATASET = "%s/project/%s/dataset/%s/version/%s";

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

            var pool = job.getResourcePool();
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
        List<RuntimeResource> runtimeResources = task.getTaskRequest().getRuntimeResources();
        var pool = task.getStep().getJob().getResourcePool();
        if (pool != null) {
            runtimeResources = pool.patchResources(runtimeResources);
        }
        if (!CollectionUtils.isEmpty(runtimeResources)) {
            return new ResourceOverwriteSpec(runtimeResources);
        }
        return null;

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
        var datasetUri = swJob.getDataSets().stream()
                    .map(dataSet -> String.format(
                            FORMATTER_URI_DATASET,
                            instanceUri,
                            project.getName(),
                            dataSet.getName(),
                            dataSet.getVersion())
                    ).collect(Collectors.joining(" "));
        coreContainerEnvs.put("SW_DATASET_URI", datasetUri);
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

        // GPU resource
        var resources = task.getTaskRequest().getRuntimeResources().stream();
        var gpu = resources.anyMatch(r -> r.getType().equals(ResourceOverwriteSpec.RESOURCE_GPU) && r.getRequest() > 0);
        // overwrite visible devices to none
        if (!gpu) {
            // https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/user-guide.html#gpu-enumeration
            coreContainerEnvs.put("NVIDIA_VISIBLE_DEVICES", "");
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
    private List<V1EnvVar> getInitContainerEnvs(Task task) {
        Job swJob = task.getStep().getJob();
        JobRuntime jobRuntime = swJob.getJobRuntime();

        List<String> downloads = new ArrayList<>();
        try {
            storageAccessService.list(swJob.getModel().getPath()).forEach(path -> {
                try {
                    String modelSignedUrl = storageAccessService.signedUrl(path, 1000 * 60 * 60L);
                    downloads.add(modelSignedUrl);
                } catch (IOException e) {
                    throw new SwProcessException(ErrorType.STORAGE, "sign model url failed for " + path, e);
                }
            });
        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE,
                    "list model files failed for " + swJob.getModel().getPath(),
                    e);
        }
        try {
            storageAccessService.list(jobRuntime.getStoragePath()).forEach(path -> {
                try {
                    String runtimeSignedUrl = storageAccessService.signedUrl(path,
                            1000 * 60 * 60L);
                    downloads.add(runtimeSignedUrl);
                } catch (IOException e) {
                    throw new SwProcessException(ErrorType.STORAGE,
                            "sign runtime url failed for " + path,
                            e);
                }

            });

        } catch (IOException e) {
            throw new SwProcessException(ErrorType.STORAGE,
                    "list runtime url failed for " + jobRuntime.getStoragePath(),
                    e);
        }
        return mapToEnv(Map.of("DOWNLOADS", Strings.join(downloads, ' ')));
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
