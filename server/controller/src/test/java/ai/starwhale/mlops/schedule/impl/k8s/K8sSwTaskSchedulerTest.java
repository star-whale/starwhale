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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.dataset.bo.DataSet;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.Resource;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.system.resourcepool.bo.Toleration;
import ai.starwhale.mlops.domain.task.bo.ResultPath;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFinder;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class K8sSwTaskSchedulerTest {


    @Test
    public void testScheduler() throws IOException, ApiException {
        K8sClient k8sClient = mock(K8sClient.class);
        K8sSwTaskScheduler scheduler = buildK8sScheduler(k8sClient);
        scheduler.schedule(Set.of(mockTask(false)), mock(TaskReportReceiver.class));
        verify(k8sClient).deployJob(any());
    }

    @NotNull
    private K8sSwTaskScheduler buildK8sScheduler(K8sClient k8sClient) throws IOException {

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
        return new K8sSwTaskScheduler(k8sClient,
                new K8sJobTemplateMock(""),
                tcsFinder,
                "rp",
                10,
                storageAccessService,
                mock(ThreadPoolTaskScheduler.class));
    }

    @Test
    public void testException() throws ApiException, IOException {
        K8sClient k8sClient = mock(K8sClient.class);
        when(k8sClient.deployJob(any())).thenThrow(new ApiException());
        K8sSwTaskScheduler scheduler = buildK8sScheduler(k8sClient);
        Task task = mockTask(false);
        scheduler.schedule(Set.of(task), mock(TaskReportReceiver.class));
        Assertions.assertEquals(TaskStatus.FAIL, task.getStatus());
    }

    @Test
    public void testRenderWithDefaultGpuResourceInPool() throws IOException, ApiException {
        var client = mock(K8sClient.class);

        var scheduler = buildK8sScheduler(client);
        var task = mockTask(false);
        var pool = task.getStep().getResourcePool();
        // add GPU resource
        var resources = List.of(new Resource(ResourceOverwriteSpec.RESOURCE_CPU, 1f, 0f, 1f));
        pool.setResources(resources);

        var jobArgumentCaptor = ArgumentCaptor.forClass(V1Job.class);
        // set no resource spec in task
        task.getTaskRequest().setRuntimeResources(List.of());
        scheduler.schedule(Set.of(task), mock(TaskReportReceiver.class));

        verify(client, times(1)).deployJob(jobArgumentCaptor.capture());
        var jobs = jobArgumentCaptor.getAllValues();
        var expectedEnv = new V1EnvVar().name("NVIDIA_VISIBLE_DEVICES").value("");

        // should use the GPU resource by default
        Assertions.assertFalse(jobs.get(0).getSpec().getTemplate().getSpec()
                .getContainers().get(0).getEnv().contains(expectedEnv));
    }

    @Test
    public void testDevMode() throws IOException, ApiException {
        var client = mock(K8sClient.class);

        var scheduler = buildK8sScheduler(client);
        var task = mockTask(true);
        scheduler.schedule(Set.of(task), mock(TaskReportReceiver.class));
        var jobArgumentCaptor = ArgumentCaptor.forClass(V1Job.class);

        verify(client, times(1)).deployJob(jobArgumentCaptor.capture());
        var jobs = jobArgumentCaptor.getAllValues();
        var container = jobs.get(0).getSpec().getTemplate().getSpec().getContainers().get(0);
        Assertions.assertEquals(List.of("dev"), container.getArgs());
    }

    private Task mockTask(boolean devMode) {
        Job job = Job.builder()
                .id(1L)
                .model(Model.builder().name("swmpN").version("swmpV").projectId(101L).build())
                .jobRuntime(JobRuntime.builder()
                        .name("swrtN")
                        .version("swrtV")
                        .image("testimage")
                        .projectId(102L)
                        .manifest(new RuntimeService.RuntimeManifest(
                                "",
                                new RuntimeService.RuntimeManifest.Environment("3.10",
                                        new RuntimeService.RuntimeManifest.Lock("0.5.1")), null))
                        .build())
                .type(JobType.EVALUATION)
                .devMode(devMode)
                .uuid("juuid")
                .dataSets(
                        List.of(DataSet.builder()
                                .indexTable("it").path("swds_path").name("swdsN").version("swdsV")
                                .size(300L).projectId(103L).build()))
                .stepSpec("")
                .resourcePool(ResourcePool.builder().name("bj01").build())
                .project(Project.builder().name("project").id(100L).build())
                .build();
        Step step = new Step();
        step.setId(1L);
        step.setName("cmp");
        step.setJob(job);
        step.setResourcePool(job.getResourcePool());
        return Task.builder()
                .id(1L)
                .taskRequest(TaskRequest.builder().index(1).total(2).build())
                .step(step)
                .resultRootPath(new ResultPath("task"))
                .uuid("uuid")
                .status(TaskStatus.READY)
                .taskRequest(TaskRequest.builder()
                        .index(1)
                        .total(1)
                        .runtimeResources(List.of(new RuntimeResource("cpu", 1f, 1f)))
                        .env(List.of(Env.builder().name("SW_ENV").value("test").build()))
                        .build())
                .build();
    }

    public static class K8sJobTemplateMock extends K8sJobTemplate {

        public K8sJobTemplateMock(String templatePath) throws IOException {
            super("", "", "/path");
        }

        @Override
        public V1Job renderJob(V1Job job, String jobName, String restartPolicy, int backoffLimit,
                Map<String, ContainerOverwriteSpec> containerSpecMap,
                Map<String, String> nodeSelectors, List<Toleration> tolerations, Map<String, String> annotations) {
            ContainerOverwriteSpec worker = containerSpecMap.get("worker");
            Assertions.assertIterableEquals(worker.getCmds(), List.of("run"));
            Assertions.assertEquals("testimage", worker.getImage());
            Assertions.assertIterableEquals(Map.of("cpu", new Quantity("1000m")).entrySet(),
                    worker.getResourceOverwriteSpec().getResourceSelector().getRequests().entrySet());
            return null;
        }

    }

    @Test
    public void testExec() throws ApiException, IOException, InterruptedException, ExecutionException {
        var client = mock(K8sClient.class);
        var restartPolicy = "";
        var backoffLimit = 2;

        var threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();

        var scheduler = new K8sSwTaskScheduler(
                client,
                mock(K8sJobTemplate.class),
                mock(TaskContainerSpecificationFinder.class),
                restartPolicy,
                backoffLimit,
                mock(StorageAccessService.class),
                threadPoolTaskScheduler);

        var task = Task.builder().id(7L).build();
        var podList = new V1PodList();
        podList.setItems(List.of());
        when(client.getPodsByJobName("7")).thenReturn(podList);

        // exec will throw exception if pod not found
        assertThrows(SwProcessException.class, () -> scheduler.exec(task, "ls"));

        var pod = new V1Pod();
        pod.setMetadata(new V1ObjectMeta());
        pod.getMetadata().setName("7");
        podList.setItems(List.of(pod));

        when(client.getPodsByJobName("7")).thenReturn(podList);
        when(client.execInPod("7", null, "ls")).thenReturn(new String[]{"stdout", "stderr"});
        var resp = scheduler.exec(task, "ls").get();
        verify(client).execInPod("7", null, "ls");
        assertEquals("stdout", resp[0]);
        assertEquals("stderr", resp[1]);
    }

    @Test
    public void testStop() throws ApiException {
        var client = mock(K8sClient.class);
        var restartPolicy = "";
        var backoffLimit = 2;

        var threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();

        var scheduler = new K8sSwTaskScheduler(
                client,
                mock(K8sJobTemplate.class),
                mock(TaskContainerSpecificationFinder.class),
                restartPolicy,
                backoffLimit,
                mock(StorageAccessService.class),
                threadPoolTaskScheduler);

        var task = Task.builder().id(7L).build();
        scheduler.stop(List.of(task));
        // make sure the job is deleted even if exception occurs when collecting logs
        verify(client).deleteJob("7");
    }
}
