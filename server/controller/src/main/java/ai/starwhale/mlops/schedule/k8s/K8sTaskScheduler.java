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

import ai.starwhale.mlops.api.protocol.report.resp.SwRunTime;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.JobTokenConfig;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.swds.bo.SwDataSet;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForJobStatus;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForLogging;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForSchedule;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.storage.configuration.StorageProperties;
import ai.starwhale.mlops.storage.fs.FileStorageEnv;
import cn.hutool.json.JSONUtil;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Node;
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class K8sTaskScheduler implements SwTaskScheduler {

    final K8sClient k8sClient;

    final K8sResourcePoolConverter resourcePoolConverter;

    final StorageProperties storageProperties;

    final RunTimeProperties runTimeProperties;

    final JobTokenConfig jobTokenConfig;

    final K8sJobTemplate k8sJobTemplate;
    final ResourceEventHandler<V1Job> eventHandlerJob;
    final ResourceEventHandler<V1Node> eventHandlerNode;
    final String instanceUri;

    public K8sTaskScheduler(K8sClient k8sClient,
            StorageProperties storageProperties,
            JobTokenConfig jobTokenConfig,
            RunTimeProperties runTimeProperties,
            K8sResourcePoolConverter resourcePoolConverter,
            K8sJobTemplate k8sJobTemplate,
            ResourceEventHandler<V1Job> eventHandlerJob,
            ResourceEventHandler<V1Node> eventHandlerNode, @Value("${sw.instance-uri}") String instanceUri) {
        this.k8sClient = k8sClient;
        this.storageProperties = storageProperties;
        this.jobTokenConfig = jobTokenConfig;
        this.runTimeProperties = runTimeProperties;
        this.resourcePoolConverter = resourcePoolConverter;
        this.k8sJobTemplate = k8sJobTemplate;
        this.eventHandlerJob = eventHandlerJob;
        this.eventHandlerNode = eventHandlerNode;
        this.instanceUri = instanceUri;
    }

    @Override
    public void schedule(Collection<Task> tasks,
            Clazz deviceClass) {
        tasks.parallelStream().forEach(this::deployTaskToK8s);
    }

    @Override
    public void stopSchedule(Collection<Long> taskIds) {
        taskIds.parallelStream().forEach(id -> {
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

    private void deployTaskToK8s(Task task) {
        log.debug("deploying task to k8s {} ", task.getId());
        try {
            var nodeSelector = this.resourcePoolConverter.toK8sLabel(task.getStep().getJob().getResourcePool());
            V1Job k8sJob = k8sJobTemplate.renderJob(task.getId().toString(), buildContainerSpecMap(task), nodeSelector);
            log.debug("deploying k8sJob to k8s :{}", JSONUtil.toJsonStr(k8sJob));
            k8sClient.deploy(k8sJob);
        } catch (ApiException k8sE) {
            log.error(" schedule task failed {}", k8sE.getResponseBody(), k8sE);
            taskFailed(task);
        } catch (Exception e) {
            log.error(" schedule task failed ", e);
            taskFailed(task);
        }
    }

    private Map<String, ContainerOverwriteSpec> buildContainerSpecMap(Task task) {

        Map<String, String> initContainerEnvs = getInitContainerEnvs(task);
        // task container envs
        Map<String, String> coreContainerEnvs = buildCoreContainerEnvs(task);
        // TODO: use task's resource needs
        Map<String, ContainerOverwriteSpec> ret = new HashMap<>();
        k8sJobTemplate.getInitContainerTemplates().forEach(templateContainer -> {
            ContainerOverwriteSpec containerOverwriteSpec = new ContainerOverwriteSpec(templateContainer.getName());
            containerOverwriteSpec.setEnvs(mapToEnv(initContainerEnvs));
            ret.put(templateContainer.getName(), containerOverwriteSpec);
        });

        JobRuntime jobRuntime = task.getStep().getJob().getJobRuntime();
        k8sJobTemplate.getContainersTemplates().forEach(templateContainer -> {
            ContainerOverwriteSpec containerOverwriteSpec = new ContainerOverwriteSpec(templateContainer.getName());
            containerOverwriteSpec.setEnvs(mapToEnv(coreContainerEnvs));
            containerOverwriteSpec.setCmds(List.of("run"));
            containerOverwriteSpec.setResourceOverwriteSpec(getResourceSpec(task));
            containerOverwriteSpec.setImage(jobRuntime.getImage());
            ret.put(templateContainer.getName(), containerOverwriteSpec);
        });
        return ret;
    }

    private ResourceOverwriteSpec getResourceSpec(Task task) {
        JobRuntime jobRuntime = task.getStep().getJob().getJobRuntime();
        if (null != jobRuntime.getDeviceClass() && null != jobRuntime.getDeviceAmount()) {
            return new ResourceOverwriteSpec(
                    jobRuntime.getDeviceClass(),
                    jobRuntime.getDeviceAmount().toString() + "m");
        }
        List<RuntimeResource> runtimeResources = task.getTaskRequest().getRuntimeResources();
        if (!CollectionUtils.isEmpty(runtimeResources)) {
            return new ResourceOverwriteSpec(runtimeResources);
        }
        return null;

    }

    @NotNull
    private Map<String, String> buildCoreContainerEnvs(Task task) {
        Job swJob = task.getStep().getJob();
        Map<String, String> coreContainerEnvs = new HashMap<>();
        coreContainerEnvs.put("SW_TASK_STEP", task.getStep().getName());
        // TODO: support multi dataset uris
        // oss env
        List<SwDataSet> swDataSets = swJob.getSwDataSets();
        SwDataSet swDataSet = swDataSets.get(0);
        coreContainerEnvs.put("SW_DATASET_URI", String.format(FORMATTER_URI_DATASET, instanceUri,
                swJob.getProject().getName(), swDataSet.getName(), swDataSet.getVersion()));
        coreContainerEnvs.put("SW_TASK_INDEX", String.valueOf(task.getTaskRequest().getIndex()));
        coreContainerEnvs.put("SW_EVALUATION_VERSION", swJob.getUuid());

        swDataSets.forEach(ds -> ds.getFileStorageEnvs().values()
                .forEach(fileStorageEnv -> coreContainerEnvs.putAll(fileStorageEnv.getEnvs())));

        coreContainerEnvs.put(FileStorageEnv.ENV_KEY_PREFIX, swDataSet.getPath());

        // datastore env
        coreContainerEnvs.put("SW_TOKEN", jobTokenConfig.getToken());
        coreContainerEnvs.put("SW_INSTANCE_URI", instanceUri);
        coreContainerEnvs.put("SW_PROJECT", swJob.getProject().getName());
        return coreContainerEnvs;
    }

    @NotNull
    private Map<String, String> getInitContainerEnvs(Task task) {
        Job swJob = task.getStep().getJob();
        JobRuntime jobRuntime = swJob.getJobRuntime();
        Map<String, String> initContainerEnvs = new HashMap<>();
        List<String> downloads = new ArrayList<>();
        String prefix = "s3://" + storageProperties.getS3Config().getBucket() + "/";
        downloads.add(prefix + swJob.getSwmp().getPath() + ";/opt/starwhale/swmp/");
        downloads.add(prefix + SwRunTime.builder().name(jobRuntime.getName()).version(jobRuntime.getVersion()).path(
                jobRuntime.getStoragePath()).build().getPath() + ";/opt/starwhale/swrt/");
        initContainerEnvs.put("DOWNLOADS", Strings.join(downloads, ' '));
        initContainerEnvs.put("ENDPOINT_URL", storageProperties.getS3Config().getEndpoint());
        initContainerEnvs.put("AWS_ACCESS_KEY_ID", storageProperties.getS3Config().getAccessKey());
        initContainerEnvs.put("AWS_SECRET_ACCESS_KEY", storageProperties.getS3Config().getSecretKey());
        initContainerEnvs.put("AWS_S3_REGION", storageProperties.getS3Config().getRegion());
        initContainerEnvs.put("SW_PYPI_INDEX_URL", runTimeProperties.getPypi().getIndexUrl());
        initContainerEnvs.put("SW_PYPI_EXTRA_INDEX_URL", runTimeProperties.getPypi().getExtraIndexUrl());
        initContainerEnvs.put("SW_PYPI_TRUSTED_HOST", runTimeProperties.getPypi().getTrustedHost());
        return initContainerEnvs;
    }

    @NotNull
    private List<V1EnvVar> mapToEnv(Map<String, String> initContainerEnvs) {
        return initContainerEnvs.entrySet().stream()
                .map(entry -> new V1EnvVar().name(entry.getKey()).value(entry.getValue()))
                .collect(Collectors.toList());
    }

    private void taskFailed(Task task) {
        TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(
                Set.of(TaskWatcherForJobStatus.class, TaskWatcherForSchedule.class, TaskWatcherForLogging.class));
        // todo save log
        task.updateStatus(TaskStatus.FAIL);
        TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
    }


    @EventListener
    public void handleContextReadyEvent(ApplicationReadyEvent ctxReadyEvt) {
        log.info("spring context ready now");
        k8sClient.watchJob(eventHandlerJob, K8sClient.toV1LabelSelector(K8sJobTemplate.starwhaleJobLabel));
        k8sClient.watchNode(eventHandlerNode);
    }
}
