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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.RunTimeProperties.ImageBuild;
import ai.starwhale.mlops.configuration.RunTimeProperties.Pypi;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.dataset.bo.DataSet;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.system.resourcepool.bo.Resource;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.system.resourcepool.bo.Toleration;
import ai.starwhale.mlops.domain.task.bo.ResultPath;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.watchers.log.TaskLogK8sCollector;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class K8sTaskSchedulerTest {

    @Test
    public void testScheduler() throws IOException, ApiException {
        K8sClient k8sClient = mock(K8sClient.class);
        K8sTaskScheduler scheduler = buildK8sScheduler(k8sClient);
        scheduler.schedule(Set.of(mockTask(false)));
        verify(k8sClient).deployJob(any());
    }

    @NotNull
    private K8sTaskScheduler buildK8sScheduler(K8sClient k8sClient) throws IOException {
        TaskTokenValidator taskTokenValidator = mock(TaskTokenValidator.class);
        when(taskTokenValidator.getTaskToken(any(), any())).thenReturn("tt");
        RunTimeProperties runTimeProperties = new RunTimeProperties(
                "", new ImageBuild(), new Pypi("indexU", "extraU", "trustedH"));
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        when(storageAccessService.list(eq("path_rt"))).thenReturn(Stream.of("path_rt"));
        when(storageAccessService.signedUrl(eq("path_rt"), any())).thenReturn("s3://bucket/path_rt");
        return new K8sTaskScheduler(k8sClient,
                taskTokenValidator,
                runTimeProperties,
                new K8sJobTemplateMock(""),
                "http://instanceUri",
                 8000,
                 50,
                "OnFailure", 10,
                storageAccessService,
                mock(TaskLogK8sCollector.class),
                mock(ThreadPoolTaskScheduler.class)
        );
    }

    @Test
    public void testException() throws ApiException, IOException {
        K8sClient k8sClient = mock(K8sClient.class);
        when(k8sClient.deployJob(any())).thenThrow(new ApiException());
        K8sTaskScheduler scheduler = buildK8sScheduler(k8sClient);
        Task task = mockTask(false);
        scheduler.schedule(Set.of(task));
        Assertions.assertEquals(TaskStatus.FAIL, task.getStatus());
    }

    @Test
    public void testRenderWithoutGpuResource() throws IOException, ApiException {
        var client = mock(K8sClient.class);

        var runTimeProperties = new RunTimeProperties("", new ImageBuild(), new Pypi("", "", ""));
        var k8sJobTemplate = new K8sJobTemplate("", "", "", "");
        var scheduler = new K8sTaskScheduler(
                client,
                mock(TaskTokenValidator.class),
                runTimeProperties,
                k8sJobTemplate,
                "",
                8000,
                50,
                "OnFailure",
                10,
                mock(StorageAccessService.class),
                mock(TaskLogK8sCollector.class),
                mock(ThreadPoolTaskScheduler.class)
        );
        var task = mockTask(false);
        scheduler.schedule(Set.of(task));
        var jobArgumentCaptor = ArgumentCaptor.forClass(V1Job.class);
        task.getTaskRequest()
                .setRuntimeResources(List.of(new RuntimeResource(ResourceOverwriteSpec.RESOURCE_GPU, 1f, 0f)));
        scheduler.schedule(Set.of(task));

        verify(client, times(2)).deployJob(jobArgumentCaptor.capture());
        var jobs = jobArgumentCaptor.getAllValues();
        var expectedEnv = new V1EnvVar().name("NVIDIA_VISIBLE_DEVICES").value("");
        Assertions.assertTrue(jobs.get(0).getSpec().getTemplate().getSpec()
                .getContainers().get(0).getEnv().contains(expectedEnv));
        Assertions.assertFalse(jobs.get(1).getSpec().getTemplate().getSpec()
                .getContainers().get(0).getEnv().contains(expectedEnv));
    }

    @Test
    public void testRenderWithDefaultGpuResourceInPool() throws IOException, ApiException {
        var client = mock(K8sClient.class);

        var runTimeProperties = new RunTimeProperties("", new ImageBuild(), new Pypi("", "", ""));
        var k8sJobTemplate = new K8sJobTemplate("", "", "", "");
        var scheduler = new K8sTaskScheduler(
                client,
                mock(TaskTokenValidator.class),
                runTimeProperties,
                k8sJobTemplate,
                "",
                8000,
                50,
                "OnFailure",
                10,
                mock(StorageAccessService.class),
                mock(TaskLogK8sCollector.class),
                mock(ThreadPoolTaskScheduler.class)
        );
        var task = mockTask(false);
        var pool = task.getStep().getResourcePool();
        // add GPU resource
        var resources = List.of(new Resource(ResourceOverwriteSpec.RESOURCE_GPU, 1f, 0f, 1f));
        pool.setResources(resources);

        var jobArgumentCaptor = ArgumentCaptor.forClass(V1Job.class);
        // set no resource spec in task
        task.getTaskRequest().setRuntimeResources(List.of());
        scheduler.schedule(Set.of(task));

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

        var runTimeProperties = new RunTimeProperties("", new ImageBuild(), new Pypi("", "", ""));
        var k8sJobTemplate = new K8sJobTemplate("", "", "", "");
        var scheduler = new K8sTaskScheduler(
                client,
                mock(TaskTokenValidator.class),
                runTimeProperties,
                k8sJobTemplate,
                "",
                8000,
                50,
                "OnFailure",
                10,
                mock(StorageAccessService.class),
                mock(TaskLogK8sCollector.class),
                mock(ThreadPoolTaskScheduler.class)
        );
        var task = mockTask(true);
        scheduler.schedule(Set.of(task));
        var jobArgumentCaptor = ArgumentCaptor.forClass(V1Job.class);

        verify(client, times(1)).deployJob(jobArgumentCaptor.capture());
        var jobs = jobArgumentCaptor.getAllValues();
        var container = jobs.get(0).getSpec().getTemplate().getSpec().getContainers().get(0);
        Assertions.assertEquals(List.of("dev"), container.getArgs());
    }

    private Task mockTask(boolean devMode) {
        Job job = Job.builder()
                .id(1L)
                .model(Model.builder().name("swmpN").version("swmpV").build())
                .jobRuntime(JobRuntime.builder()
                                .name("swrtN").version("swrtV").image("imageRT").storagePath("path_rt")
                                .build())
                .type(JobType.EVALUATION)
                .devMode(devMode)
                .uuid("juuid")
                .dataSets(
                        List.of(DataSet.builder().indexTable("it").path("swds_path").name("swdsN").version("swdsV")
                                .size(300L).build()))
                .stepSpec("")
                .resourcePool(ResourcePool.builder().name("bj01").build())
                .project(Project.builder().name("project").id(7L).build())
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
            super("", "", "", "/path");
        }

        @Override
        public V1Job renderJob(V1Job job, String jobName, String restartPolicy, int backoffLimit,
                Map<String, ContainerOverwriteSpec> containerSpecMap,
                Map<String, String> nodeSelectors, List<Toleration> tolerations, Map<String, String> annotations) {
            ContainerOverwriteSpec worker = containerSpecMap.get("worker");
            Assertions.assertIterableEquals(worker.getCmds(), List.of("run"));
            Assertions.assertEquals("imageRT", worker.getImage());
            Assertions.assertIterableEquals(Map.of("cpu", new Quantity("1000m")).entrySet(),
                    worker.getResourceOverwriteSpec().getResourceSelector().getRequests().entrySet());
            Map<String, String> expectedEnvs = new HashMap<>() {
            };
            expectedEnvs.put("SW_ENV", "test");
            expectedEnvs.put("SW_PROJECT", "project");
            expectedEnvs.put("DATASET_CONSUMPTION_BATCH_SIZE", "50");
            expectedEnvs.put("SW_DATASET_URI", "http://instanceUri/project/project/dataset/swdsN/version/swdsV");
            expectedEnvs.put("SW_MODEL_URI", "http://instanceUri/project/project/model/swmpN/version/swmpV");
            expectedEnvs.put("SW_RUNTIME_URI", "http://instanceUri/project/project/runtime/swrtN/version/swrtV");
            expectedEnvs.put("SW_MODEL_VERSION", "swmpN/version/swmpV");
            expectedEnvs.put("SW_RUNTIME_VERSION", "swrtN/version/swrtV");
            expectedEnvs.put("SW_TASK_INDEX", "1");
            expectedEnvs.put("SW_TASK_NUM", "1");
            expectedEnvs.put("SW_PYPI_INDEX_URL", "indexU");
            expectedEnvs.put("SW_PYPI_EXTRA_INDEX_URL", "extraU");
            expectedEnvs.put("SW_PYPI_TRUSTED_HOST", "trustedH");
            expectedEnvs.put("SW_JOB_VERSION", "juuid");
            expectedEnvs.put("SW_TOKEN", "tt");
            expectedEnvs.put("SW_INSTANCE_URI", "http://instanceUri");
            expectedEnvs.put("SW_TASK_STEP", "cmp");
            expectedEnvs.put("NVIDIA_VISIBLE_DEVICES", "");
            Map<String, String> actualEnv = worker.getEnvs().stream()
                    .filter(envVar -> envVar.getValue() != null)
                    .collect(Collectors.toMap(V1EnvVar::getName, V1EnvVar::getValue));
            assertMapEquals(expectedEnvs, actualEnv);
            return null;
        }

        private void assertMapEquals(Map<String, String> expectedEnvs, Map<String, String> actualEnv) {
            Assertions.assertEquals(expectedEnvs.size(), actualEnv.size());
            expectedEnvs.forEach((k, v) -> Assertions.assertEquals(v, actualEnv.get(k)));
        }
    }

    @Test
    public void testExec() throws ApiException, IOException, InterruptedException, ExecutionException {
        var client = mock(K8sClient.class);
        var instanceUri = "";
        var devPort = 8080;
        var datasetLoadBatchSize = 10;
        var restartPolicy = "";
        var backoffLimit = 2;

        var threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();

        var scheduler = new K8sTaskScheduler(
                client,
                mock(TaskTokenValidator.class),
                mock(RunTimeProperties.class),
                mock(K8sJobTemplate.class),
                instanceUri,
                devPort,
                datasetLoadBatchSize,
                restartPolicy,
                backoffLimit,
                mock(StorageAccessService.class),
                mock(TaskLogK8sCollector.class),
                threadPoolTaskScheduler
        );

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
        when(client.execInPod("7", null, "ls")).thenReturn(new String[] {"stdout", "stderr"});
        var resp = scheduler.exec(task, "ls").get();
        verify(client).execInPod("7", null, "ls");
        assertEquals("stdout", resp[0]);
        assertEquals("stderr", resp[1]);
    }
}
