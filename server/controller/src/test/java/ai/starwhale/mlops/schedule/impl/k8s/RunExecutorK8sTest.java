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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunSpec;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.system.resourcepool.bo.Toleration;
import ai.starwhale.mlops.domain.task.bo.ResultPath;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFinder;
import ai.starwhale.mlops.schedule.reporting.ReportedRun;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class RunExecutorK8sTest {

    @Test
    public void testScheduler() throws IOException, ApiException {
        K8sClient k8sClient = mock(K8sClient.class);
        RunExecutorK8s scheduler = buildK8sScheduler(k8sClient);
        scheduler.run(mockRun(), mock(RunReportReceiver.class));
        verify(k8sClient).deployJob(any());
    }

    @NotNull
    private RunExecutorK8s buildK8sScheduler(K8sClient k8sClient) throws IOException {

        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        when(storageAccessService.list(eq("path_rt"))).thenReturn(Stream.of("path_rt"));
        when(storageAccessService.signedUrl(eq("path_rt"), any())).thenReturn("s3://bucket/path_rt");
        TaskContainerSpecificationFinder tcsFinder = mock(TaskContainerSpecificationFinder.class);
        ContainerSpecification containerSpecification = mock(ContainerSpecification.class);
        when(tcsFinder.findCs(any())).thenReturn(containerSpecification);
        when(containerSpecification.getContainerEnvs()).thenReturn(Map.of("NVIDIA_VISIBLE_DEVICES", ""));
        when(containerSpecification.getCmd()).thenReturn(
                new ContainerCommand(new String[]{"run"}, new String[]{"bash", "entrypoint.sh"}));
        when(containerSpecification.getImage()).thenReturn("testimage");
        return new RunExecutorK8s(
                k8sClient,
                new K8sJobTemplateMock(""),
                "rp",
                mock(ThreadPoolTaskScheduler.class)
        );
    }

    @Test
    public void testException() throws ApiException, IOException {
        K8sClient k8sClient = mock(K8sClient.class);
        when(k8sClient.deployJob(any())).thenThrow(new ApiException());
        RunExecutorK8s scheduler = buildK8sScheduler(k8sClient);
        Run run = mockRun();
        RunReportReceiver reportReceiver = mock(RunReportReceiver.class);
        scheduler.run(run, reportReceiver);
        ArgumentCaptor<ReportedRun> argumentCaptor = ArgumentCaptor.forClass(ReportedRun.class);
        verify(reportReceiver).receive(argumentCaptor.capture());
        Assertions.assertEquals(RunStatus.FAILED, argumentCaptor.getValue().getStatus());
    }

    private Run mockRun() {
        return Run.builder()
                .id(1L)
                .logDir(new ResultPath("task").logDir())
                .runSpec(RunSpec.builder()
                                 .resourcePool(ResourcePool.builder().name("bj01").build())
                                 .requestedResources(List.of(new RuntimeResource("cpu", 1f, 1f)))
                                 .envs(Map.of("SW_ENV", "test"))
                                 .image("testimage")
                                 .command(new ContainerCommand(
                                         new String[]{"run"},
                                         new String[]{"bash", "entrypoint.sh"}
                                 ))
                                 .build())
                .build();
    }

    public static class K8sJobTemplateMock extends K8sJobTemplate {

        public K8sJobTemplateMock(String templatePath) throws IOException {
            super("", "/path");
        }

        @Override
        public V1Job renderJob(
                V1Job job, String jobName, String restartPolicy,
                Map<String, ContainerOverwriteSpec> containerSpecMap,
                Map<String, String> nodeSelectors, List<Toleration> tolerations,
                Map<String, String> annotations
        ) {
            ContainerOverwriteSpec worker = containerSpecMap.get("worker");
            Assertions.assertIterableEquals(worker.getCmds(), List.of("run"));
            Assertions.assertEquals("testimage", worker.getImage());
            Assertions.assertIterableEquals(
                    Map.of("cpu", new Quantity("1000m")).entrySet(),
                    worker.getResourceOverwriteSpec().getResourceSelector().getRequests().entrySet()
            );
            super.renderJob(job, jobName, restartPolicy, containerSpecMap, nodeSelectors, tolerations,
                            annotations
            );
            return null;
        }

    }

    @Test
    public void testExec() throws ApiException, IOException, InterruptedException, ExecutionException {
        var client = mock(K8sClient.class);
        var restartPolicy = "";

        var threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();

        var scheduler = new RunExecutorK8s(
                client,
                mock(K8sJobTemplate.class),
                restartPolicy,
                threadPoolTaskScheduler
        );

        var run = Run.builder().id(7L).build();
        var podList = new V1PodList();
        podList.setItems(List.of());
        when(client.getPodsByJobName("7")).thenReturn(podList);

        // exec will throw exception if pod not found
        assertThrows(SwProcessException.class, () -> scheduler.exec(run, "ls"));

        var pod = new V1Pod();
        pod.setMetadata(new V1ObjectMeta());
        pod.getMetadata().setName("7");
        podList.setItems(List.of(pod));

        when(client.getPodsByJobName("7")).thenReturn(podList);
        when(client.execInPod("7", null, "ls")).thenReturn(new String[]{"stdout", "stderr"});
        var resp = scheduler.exec(run, "ls").get();
        verify(client).execInPod("7", null, "ls");
        assertEquals("stdout", resp[0]);
        assertEquals("stderr", resp[1]);
    }

    @Test
    public void testStop() throws ApiException {
        var client = mock(K8sClient.class);
        var restartPolicy = "";

        var threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();

        var scheduler = new RunExecutorK8s(
                client,
                mock(K8sJobTemplate.class),
                restartPolicy,
                threadPoolTaskScheduler
        );

        var run = Run.builder().id(7L).build();
        scheduler.stop(run);
        // make sure the job is deleted even if exception occurs when collecting logs
        verify(client).deleteJob("7");
    }
}
