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

package ai.starwhale.mlops.schedule.impl.k8s;

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.system.resourcepool.bo.Toleration;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForSchedule;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFinder;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.json.JSONUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectFieldSelector;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;

@Slf4j
public class K8sSwTaskScheduler implements SwTaskScheduler {

    final K8sClient k8sClient;

    final K8sJobTemplate k8sJobTemplate;

    final TaskContainerSpecificationFinder taskContainerSpecificationFinder;
    final String restartPolicy;
    final int backoffLimit;
    final StorageAccessService storageAccessService;
    final ThreadPoolTaskScheduler cmdExecThreadPool;


    public K8sSwTaskScheduler(
            K8sClient k8sClient,
            K8sJobTemplate k8sJobTemplate,
            TaskContainerSpecificationFinder taskContainerSpecificationFinder,
            String restartPolicy,
            Integer backoffLimit,
            StorageAccessService storageAccessService,
            ThreadPoolTaskScheduler cmdExecThreadPool) {
        this.k8sClient = k8sClient;
        this.k8sJobTemplate = k8sJobTemplate;
        this.taskContainerSpecificationFinder = taskContainerSpecificationFinder;
        this.storageAccessService = storageAccessService;
        this.restartPolicy = restartPolicy;
        this.backoffLimit = backoffLimit;
        this.cmdExecThreadPool = cmdExecThreadPool;
    }

    @Override
    public void schedule(Collection<Task> tasks, TaskReportReceiver taskReportReceiver) {
        tasks.forEach(this::deployTaskToK8s);
    }

    @Override
    public void stop(Collection<Task> tasks) {
        tasks.forEach(task -> {
            try {
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
            return cmdExecThreadPool.submit(
                    () -> k8sClient.execInPod(pods.getItems().get(0).getMetadata().getName(), null, command));
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.K8S, "exec command failed: " + e.getResponseBody(), e);
        }
    }

    static final String ANNOTATION_KEY_JOB_ID = "starwhale.ai/job-id";
    static final String ANNOTATION_KEY_TASK_ID = "starwhale.ai/task-id";
    static final String ANNOTATION_KEY_USER_ID = "starwhale.ai/user-id";
    static final String ANNOTATION_KEY_PROJECT_ID = "starwhale.ai/project-id";

    private void deployTaskToK8s(Task task) {
        log.debug("deploying task to k8s {} ", task.getId());
        ContainerSpecification containerSpecification = taskContainerSpecificationFinder.findCs(task);
        try {
            V1Job k8sJob = k8sJobTemplate.loadJob(K8sJobTemplate.WORKLOAD_TYPE_EVAL);

            // TODO: use task's resource needs
            Map<String, ContainerOverwriteSpec> containerSpecMap = new HashMap<>();

            var job = task.getStep().getJob();

            k8sJobTemplate.getContainersTemplates(k8sJob).forEach(templateContainer -> {
                ContainerOverwriteSpec containerOverwriteSpec = new ContainerOverwriteSpec(templateContainer.getName());
                containerOverwriteSpec.setEnvs(buildCoreContainerEnvs(containerSpecification.getContainerEnvs()));
                ContainerCommand containerCommand = containerSpecification.getCmd();
                containerOverwriteSpec.setCmds(
                        containerCommand.getCmd() == null ? List.of() : Arrays.asList(containerCommand.getCmd()));
                containerOverwriteSpec.setEntrypoint(
                        containerCommand.getEntrypoint() == null ? List.of()
                                : Arrays.asList(containerCommand.getEntrypoint()));
                containerOverwriteSpec.setResourceOverwriteSpec(getResourceSpec(task));
                containerOverwriteSpec.setImage(containerSpecification.getImage());
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
    private List<V1EnvVar> buildCoreContainerEnvs(Map<String, String> env) {
        var envs = mapToEnv(env);
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
