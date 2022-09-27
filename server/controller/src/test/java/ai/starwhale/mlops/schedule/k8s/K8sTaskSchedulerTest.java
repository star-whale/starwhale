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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.RunTimeProperties.Pypi;
import ai.starwhale.mlops.configuration.security.JobTokenConfig;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.swds.bo.SwDataSet;
import ai.starwhale.mlops.domain.swmp.SwModelPackage;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.storage.configuration.StorageProperties;
import ai.starwhale.mlops.storage.env.StorageEnv;
import ai.starwhale.mlops.storage.env.StorageEnv.StorageEnvType;
import ai.starwhale.mlops.storage.env.StorageEnvsPropertiesConverter;
import ai.starwhale.mlops.storage.s3.S3Config;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class K8sTaskSchedulerTest {

    public static final String bucket = "bucket";
    public static final String accessKey = "accessKey";
    public static final String secretKey = "secretKey";
    public static final String region = "region";
    public static final String endpoint = "endpoint";

    @Test
    public void testScheduler() throws IOException, ApiException {
        K8sClient k8sClient = mock(K8sClient.class);
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setS3Config(
                S3Config.builder()
                        .bucket(bucket)
                        .accessKey(accessKey)
                        .secretKey(secretKey)
                        .region(region)
                        .endpoint(endpoint)
                        .build());
        JobTokenConfig jobTokenConfig = mock(JobTokenConfig.class);
        when(jobTokenConfig.getToken()).thenReturn("tt");
        RunTimeProperties runTimeProperties = new RunTimeProperties("", new Pypi("indexU", "extraU", "trustedH"));
        K8sTaskScheduler scheduler = new K8sTaskScheduler(k8sClient,
                storageProperties,
                jobTokenConfig,
                runTimeProperties,
                new K8sResourcePoolConverter(),
                new K8sJobTemplateMock(""),
                null,
                null,
                "http://instanceUri", new StorageEnvsPropertiesConverter(storageProperties));
        scheduler.schedule(Set.of(mockTask()));
        verify(k8sClient).deploy(any());
    }

    private Task mockTask() {
        Job job = Job.builder()
                .id(1L)
                .swmp(SwModelPackage.builder().path("path_swmp").build())
                .jobRuntime(JobRuntime.builder().image("imageRT").storagePath("path_rt").build())
                .type(JobType.EVALUATION)
                .uuid("juuid")
                .swDataSets(
                        List.of(SwDataSet.builder().indexTable("it").path("swds_path").name("swdsN").version("swdsV")
                                .size(300L).fileStorageEnvs(Map.of("FS",
                                        new StorageEnv(StorageEnvType.S3).add("envS4", "envS4V"))).build()))
                .stepSpec("")
                .resourcePool(ResourcePool.builder().label("bj01").build())
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
                        .runtimeResources(List.of(new RuntimeResource("cpu", 1))).build())
                .build();
        return task;
    }

    public static class K8sJobTemplateMock extends K8sJobTemplate {

        public K8sJobTemplateMock(String templatePath) throws IOException {
            super("");
        }

        @Override
        public V1Job renderJob(String jobName,
                Map<String, ContainerOverwriteSpec> containerSpecMap,
                Map<String, String> nodeSelectors) {
            Assertions.assertIterableEquals(
                    new K8sResourcePoolConverter().toK8sLabel(ResourcePool.builder().label("bj01").build()).entrySet(),
                    nodeSelectors.entrySet());
            ContainerOverwriteSpec worker = containerSpecMap.get("worker");
            Assertions.assertIterableEquals(worker.getCmds(), List.of("run"));
            Assertions.assertEquals("imageRT", worker.getImage());
            Assertions.assertIterableEquals(Map.of("cpu", new Quantity("1000m")).entrySet(),
                    worker.getResourceOverwriteSpec().getResourceSelector().getRequests().entrySet());
            Map<String, String> expectedEnvs = new HashMap<>() {
            };
            expectedEnvs.put("SW_PROJECT", "project");
            expectedEnvs.put("SW_DATASET_URI", "http://instanceUri/project/project/dataset/swdsN/version/swdsV");
            expectedEnvs.put("SW_TASK_INDEX", "1");
            expectedEnvs.put("SW_TASK_NUM", "1");
            expectedEnvs.put("SW_PYPI_INDEX_URL", "indexU");
            expectedEnvs.put("SW_PYPI_EXTRA_INDEX_URL", "extraU");
            expectedEnvs.put("SW_PYPI_TRUSTED_HOST", "trustedH");
            expectedEnvs.put("SW_EVALUATION_VERSION", "juuid");
            expectedEnvs.put("SW_TOKEN", "tt");
            expectedEnvs.put("SW_INSTANCE_URI", "http://instanceUri");
            expectedEnvs.put("SW_TASK_STEP", "cmp");
            expectedEnvs.put("ENVS4", "envS4V");
            expectedEnvs.put(StorageEnv.ENV_KEY_PREFIX, "swds_path");
            expectedEnvs.put(StorageEnv.ENV_TYPE, "S3");
            Map<String, String> actualEnv = worker.getEnvs().stream()
                    .collect(Collectors.toMap(V1EnvVar::getName, V1EnvVar::getValue));
            assertMapEquals(expectedEnvs, actualEnv);

            ContainerOverwriteSpec dp = containerSpecMap.get("data-provider");

            Map<String, String> initEnv = Map.of("DOWNLOADS",
                    "s3://bucket/path_swmp;/opt/starwhale/swmp/ s3://bucket/path_rt;/opt/starwhale/swrt/",
                    "SW_S3_BUCKET", K8sTaskSchedulerTest.bucket,
                    "SW_S3_ENDPOINT", K8sTaskSchedulerTest.endpoint,
                    "SW_S3_SECRET", K8sTaskSchedulerTest.secretKey,
                    "SW_S3_ACCESS_KEY", K8sTaskSchedulerTest.accessKey,
                    "SW_S3_REGION", K8sTaskSchedulerTest.region,
                    StorageEnv.ENV_TYPE, "S3");
            Map<String, String> initActual = dp.getEnvs().stream().filter(env -> env.getValue() != null)
                    .collect(Collectors.toMap(V1EnvVar::getName, V1EnvVar::getValue));
            assertMapEquals(initEnv, initActual);
            ContainerOverwriteSpec ut = containerSpecMap.get("untar");
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
