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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.RunTimeProperties.Pypi;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.dataset.bo.DataSet;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.model.Model;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.settings.SettingsService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.bo.ResultPath;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.storage.StorageAccessService;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class K8sTaskSchedulerTest {

    @Test
    public void testScheduler() throws IOException, ApiException {
        K8sClient k8sClient = mock(K8sClient.class);
        K8sTaskScheduler scheduler = buildK8sSheduler(k8sClient);
        scheduler.schedule(Set.of(mockTask()));
        verify(k8sClient).deployJob(any());
    }

    @NotNull
    private K8sTaskScheduler buildK8sSheduler(K8sClient k8sClient) throws IOException {
        TaskTokenValidator taskTokenValidator = mock(TaskTokenValidator.class);
        when(taskTokenValidator.getTaskToken(any(), any())).thenReturn("tt");
        RunTimeProperties runTimeProperties = new RunTimeProperties("", new Pypi("indexU", "extraU", "trustedH"));
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        SettingsService settingsService = mock(SettingsService.class);
        when(storageAccessService.list(eq("path_swmp"))).thenReturn(Stream.of("path_swmp"));
        when(storageAccessService.list(eq("path_rt"))).thenReturn(Stream.of("path_rt"));
        when(storageAccessService.signedUrl(eq("path_swmp"), any())).thenReturn("s3://bucket/path_swmp");
        when(storageAccessService.signedUrl(eq("path_rt"), any())).thenReturn("s3://bucket/path_rt");
        K8sTaskScheduler scheduler = new K8sTaskScheduler(k8sClient,
                taskTokenValidator,
                runTimeProperties,
                new K8sJobTemplateMock(""),
                "http://instanceUri", 50,
                "OnFailure", 10,
                storageAccessService, settingsService);
        return scheduler;
    }

    @Test
    public void testException() throws ApiException, IOException {
        K8sClient k8sClient = mock(K8sClient.class);
        when(k8sClient.deployJob(any())).thenThrow(new ApiException());
        K8sTaskScheduler scheduler = buildK8sSheduler(k8sClient);
        Task task = mockTask();
        scheduler.schedule(Set.of(task));
        Assertions.assertEquals(TaskStatus.FAIL, task.getStatus());
    }

    @Test
    public void testRenderWithoutGpuResource() throws IOException, ApiException {
        var client = mock(K8sClient.class);

        var runTimeProperties = new RunTimeProperties("", new Pypi("", "", ""));
        var k8sJobTemplate = new K8sJobTemplate("", "", "", "");
        var scheduler = new K8sTaskScheduler(
                client,
                mock(TaskTokenValidator.class),
                runTimeProperties,
                k8sJobTemplate,
                "",
                50,
                "OnFailure",
                10,
                mock(StorageAccessService.class),
                mock(SettingsService.class));
        var task = mockTask();
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

    private Task mockTask() {
        Job job = Job.builder()
                .id(1L)
                .model(Model.builder().path("path_swmp").build())
                .jobRuntime(JobRuntime.builder().image("imageRT").storagePath("path_rt").build())
                .type(JobType.EVALUATION)
                .uuid("juuid")
                .dataSets(
                        List.of(DataSet.builder().indexTable("it").path("swds_path").name("swdsN").version("swdsV")
                                .size(300L).build()))
                .stepSpec("")
                .resourcePool(ResourcePool.builder().name("bj01").build())
                .project(Project.builder().name("project").build())
                .build();
        Step step = new Step();
        step.setId(1L);
        step.setName("cmp");
        step.setJob(job);
        Task task = Task.builder()
                .id(1L)
                .taskRequest(TaskRequest.builder().index(1).total(2).build())
                .step(step)
                .resultRootPath(new ResultPath("task"))
                .uuid("uuid")
                .status(TaskStatus.READY)
                .taskRequest(TaskRequest.builder()
                        .index(1)
                        .total(1)
                        .runtimeResources(List.of(new RuntimeResource("cpu", 1f, 1f))).build())
                .build();
        return task;
    }

    public static class K8sJobTemplateMock extends K8sJobTemplate {

        public K8sJobTemplateMock(String templatePath) throws IOException {
            super("", "", "", "/path");
        }

        @Override
        public V1Job renderJob(V1Job job, String jobName, String restartPolicy, int backoffLimit,
                Map<String, ContainerOverwriteSpec> containerSpecMap,
                Map<String, String> nodeSelectors) {
            ContainerOverwriteSpec worker = containerSpecMap.get("worker");
            Assertions.assertIterableEquals(worker.getCmds(), List.of("evaluation"));
            Assertions.assertEquals("imageRT", worker.getImage());
            Assertions.assertIterableEquals(Map.of("cpu", new Quantity("1000m")).entrySet(),
                    worker.getResourceOverwriteSpec().getResourceSelector().getRequests().entrySet());
            Map<String, String> expectedEnvs = new HashMap<>() {
            };
            expectedEnvs.put("SW_PROJECT", "project");
            expectedEnvs.put("DATASET_CONSUMPTION_BATCH_SIZE", "50");
            expectedEnvs.put("SW_DATASET_URI", "http://instanceUri/project/project/dataset/swdsN/version/swdsV");
            expectedEnvs.put("SW_MODEL_VERSION", "null/version/null");
            expectedEnvs.put("SW_RUNTIME_VERSION", "null/version/null");
            expectedEnvs.put("SW_TASK_INDEX", "1");
            expectedEnvs.put("SW_TASK_NUM", "1");
            expectedEnvs.put("SW_PYPI_INDEX_URL", "indexU");
            expectedEnvs.put("SW_PYPI_EXTRA_INDEX_URL", "extraU");
            expectedEnvs.put("SW_PYPI_TRUSTED_HOST", "trustedH");
            expectedEnvs.put("SW_EVALUATION_VERSION", "juuid");
            expectedEnvs.put("SW_TOKEN", "tt");
            expectedEnvs.put("SW_INSTANCE_URI", "http://instanceUri");
            expectedEnvs.put("SW_TASK_STEP", "cmp");
            expectedEnvs.put("NVIDIA_VISIBLE_DEVICES", "");
            Map<String, String> actualEnv = worker.getEnvs().stream()
                    .filter(envVar -> envVar.getValue() != null)
                    .collect(Collectors.toMap(V1EnvVar::getName, V1EnvVar::getValue));
            assertMapEquals(expectedEnvs, actualEnv);

            ContainerOverwriteSpec dp = containerSpecMap.get("data-provider");

            Map<String, String> initEnv = Map.of("DOWNLOADS",
                    "s3://bucket/path_swmp s3://bucket/path_rt");
            Map<String, String> initActual = dp.getEnvs().stream().filter(env -> env.getValue() != null)
                    .collect(Collectors.toMap(V1EnvVar::getName, V1EnvVar::getValue));
            assertMapEquals(initEnv, initActual);
            ContainerOverwriteSpec ut = containerSpecMap.get("data-provider");
            initActual = ut.getEnvs().stream().filter(env -> env.getValue() != null)
                    .collect(Collectors.toMap(V1EnvVar::getName, V1EnvVar::getValue));
            assertMapEquals(initEnv, initActual);
            return null;
        }

        private void assertMapEquals(Map<String, String> expectedEnvs, Map<String, String> actualEnv) {
            Assertions.assertEquals(expectedEnvs.size(), actualEnv.size());
            expectedEnvs.forEach((k, v) -> {
                Assertions.assertEquals(v, actualEnv.get(k));
            });
        }
    }

}
