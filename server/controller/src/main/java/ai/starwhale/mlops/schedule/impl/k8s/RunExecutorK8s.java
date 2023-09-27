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

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.system.resourcepool.bo.Toleration;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.executor.RunExecutor;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.reporting.ReportedRun;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
import cn.hutool.json.JSONUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;


@Slf4j
public class RunExecutorK8s implements RunExecutor {

    final K8sClient k8sClient;
    final K8sJobTemplate k8sJobTemplate;
    final String restartPolicy;
    final ThreadPoolTaskScheduler cmdExecThreadPool;

    public RunExecutorK8s(
            K8sClient k8sClient,
            K8sJobTemplate k8sJobTemplate,
            String restartPolicy,
            ThreadPoolTaskScheduler cmdExecThreadPool
    ) {
        this.k8sClient = k8sClient;
        this.k8sJobTemplate = k8sJobTemplate;
        this.restartPolicy = restartPolicy;
        this.cmdExecThreadPool = cmdExecThreadPool;
    }

    @Override
    public void run(Run run, RunReportReceiver reportReceiver) {
        log.debug("deploying run to k8s {} ", run.getId());
        V1Job k8sJob = k8sJobTemplate.loadJobTemplate();

        Map<String, ContainerOverwriteSpec> containerSpecMap = new HashMap<>();

        k8sJobTemplate.getContainersTemplates(k8sJob).forEach(templateContainer -> {
            ContainerOverwriteSpec containerOverwriteSpec = new ContainerOverwriteSpec(templateContainer.getName());
            containerOverwriteSpec.setEnvs(mapToEnv(run.getRunSpec().getEnvs()));
            ContainerCommand containerCommand = run.getRunSpec().getCommand();
            containerOverwriteSpec.setCmds(
                    containerCommand.getCmd() == null ? List.of() : Arrays.asList(containerCommand.getCmd()));
            containerOverwriteSpec.setEntrypoint(
                    containerCommand.getEntrypoint() == null ? List.of()
                            : Arrays.asList(containerCommand.getEntrypoint()));
            containerOverwriteSpec.setResourceOverwriteSpec(getResourceSpec(run));
            containerOverwriteSpec.setImage(run.getRunSpec().getImage());
            containerSpecMap.put(templateContainer.getName(), containerOverwriteSpec);
        });

        var pool = run.getRunSpec().getResourcePool();
        Map<String, String> nodeSelector = pool != null ? pool.getNodeSelector() : Map.of();
        List<Toleration> tolerations = pool != null ? pool.getTolerations() : null;
        var annotations = generateAnnotations(run);
        if (pool != null && !CollectionUtils.isEmpty(pool.getMetadata())) {
            annotations.putAll(pool.getMetadata());
        }

        k8sJobTemplate.renderJob(
                k8sJob,
                run.getId().toString(),
                this.restartPolicy,
                containerSpecMap,
                nodeSelector,
                tolerations,
                annotations
        );


        log.debug("deploying k8sJob to k8s :{}", JSONUtil.toJsonStr(k8sJob));
        try {
            k8sClient.deleteJob(run.getId().toString());
            log.info("existing k8s job {} deleted  before start it", run.getId());
        } catch (ApiException e) {
            log.debug("try to delete existing k8s job {} before start it, however it doesn't exist", run.getId());
        }
        try {
            k8sClient.deployJob(k8sJob);
        } catch (Throwable e) {
            log.error("k8s job deploy failed", e);
            reportReceiver.receive(
                    ReportedRun.builder()
                            .id(run.getId())
                            .failedReason("k8s job deploy failed" + e.getMessage())
                            .status(RunStatus.FAILED)
                            .stopTimeMillis(System.currentTimeMillis())
                            .build()
            );
        }
    }

    @Override
    public void stop(Run run) {
        try {
            k8sClient.deleteJob(run.getId().toString());
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.debug("delete run {} not found in k8s", run.getId());
            } else {
                log.warn("delete k8s job failed {}, {}", run.getId(), e.getResponseBody(), e);
            }
        }

    }

    @Override
    public void remove(Run run) {
        stop(run);
    }

    @Override
    public Future<String[]> exec(Run run, String... command) {
        try {
            var pods = k8sClient.getPodsByJobName(run.getId().toString());
            if (CollectionUtils.isEmpty(pods.getItems())) {
                throw new SwProcessException(ErrorType.K8S, "no pod found for run " + run.getId());
            }
            if (pods.getItems().size() != 1) {
                throw new SwProcessException(ErrorType.K8S, "multiple pods found for run " + run.getId());
            }
            return cmdExecThreadPool.submit(
                    () -> k8sClient.execInPod(pods.getItems().get(0).getMetadata().getName(), null, command));
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.K8S, "exec command failed: " + e.getResponseBody(), e);
        }
    }

    static final String ANNOTATION_KEY_RUN_ID = "starwhale.ai/run-id";

    @NotNull
    private static Map<String, String> generateAnnotations(Run run) {
        var annotations = new HashMap<String, String>();
        annotations.put(ANNOTATION_KEY_RUN_ID, run.getId().toString());
        return annotations;
    }

    private ResourceOverwriteSpec getResourceSpec(Run run) {
        var runtimeResources = getPatchedResources(run);
        if (!CollectionUtils.isEmpty(runtimeResources)) {
            return new ResourceOverwriteSpec(runtimeResources);
        }
        return null;
    }

    private List<RuntimeResource> getPatchedResources(Run run) {
        List<RuntimeResource> runtimeResources = run.getRunSpec().getRequestedResources();
        var pool = run.getRunSpec().getResourcePool();
        if (pool != null) {
            runtimeResources = pool.patchResources(runtimeResources);
        }
        return runtimeResources;
    }

    @NotNull
    private List<V1EnvVar> mapToEnv(Map<String, String> initContainerEnvs) {
        return initContainerEnvs.entrySet().stream()
                .map(entry -> new V1EnvVar().name(entry.getKey()).value(entry.getValue()))
                .collect(Collectors.toList());
    }

}
