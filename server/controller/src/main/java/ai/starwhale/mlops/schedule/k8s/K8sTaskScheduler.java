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

import ai.starwhale.mlops.api.protocol.report.resp.SWRunTime;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.JobTokenConfig;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.bo.SWDataSet;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import ai.starwhale.mlops.storage.configuration.StorageProperties;
import ai.starwhale.mlops.storage.fs.FileStorageEnv;
import cn.hutool.json.JSONUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class K8sTaskScheduler implements SWTaskScheduler {

    final K8sClient k8sClient;

    final TaskBoConverter taskConvertor;

    final K8sResourcePoolConverter resourcePoolConverter;

    final StorageProperties storageProperties;

    final RunTimeProperties runTimeProperties;

    final JobTokenConfig jobTokenConfig;

    final StoragePathCoordinator storagePathCoordinator;

    @Value("${sw.instance-uri}")
    String instanceUri;

    public K8sTaskScheduler(K8sClient k8sClient,
        TaskBoConverter taskConvertor, StorageProperties storageProperties,
        JobTokenConfig jobTokenConfig,
        StoragePathCoordinator storagePathCoordinator,
        RunTimeProperties runTimeProperties,
        K8sResourcePoolConverter resourcePoolConverter) {
        this.k8sClient = k8sClient;
        this.taskConvertor = taskConvertor;
        this.storageProperties = storageProperties;
        this.jobTokenConfig = jobTokenConfig;
        this.storagePathCoordinator = storagePathCoordinator;
        this.runTimeProperties = runTimeProperties;
        this.resourcePoolConverter = resourcePoolConverter;
    }

    @Override
    public void adopt(Collection<Task> tasks,
                      Clazz deviceClass) {

        tasks.parallelStream().forEach(task -> {
            var job = task.getStep().getJob();
            var image = job.getJobRuntime().getImage();
            var label = this.resourcePoolConverter.toK8sLabel(job.getResourcePool());
            this.deployTaskToK8s(k8sClient, image, task, label);
        });
    }

    @Override
    public void remove(Collection<Long> taskIds) {
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
    static final String FORMATTER_URI_DATASET="%s/project/%s/dataset/%s/version/%s";
    /**
     * todo hard code in this piece of code will be refactored after other core concepts being refactored
     *
     * @param client
     * @param image
     * @param task
     */
    private void deployTaskToK8s(K8sClient client, String image, Task task, Map<String, String> nodeSelector) {
        log.debug("deploying task to k8s {} {}", task.getId(),  task.getTaskType());
        Map<String, String> initContainerEnvs = new HashMap<>();
        List<String> downloads = new ArrayList<>();
        Job swJob = task.getStep().getJob();
        JobRuntime jobRuntime = swJob.getJobRuntime();
        String prefix = "s3://" + storageProperties.getS3Config().getBucket() + "/";
        downloads.add(prefix + swJob.getSwmp().getPath() + ";/opt/starwhale/swmp/");
        downloads.add(prefix + SWRunTime.builder().name(jobRuntime.getName()).version(jobRuntime.getVersion()).path(
            jobRuntime.getStoragePath()).build().getPath() + ";/opt/starwhale/swrt/");
        initContainerEnvs.put("DOWNLOADS", Strings.join(downloads, ' '));
        String input = ""; //generateConfigFile(task);
        initContainerEnvs.put("INPUT", input);
        initContainerEnvs.put("ENDPOINT_URL", storageProperties.getS3Config().getEndpoint());
        initContainerEnvs.put("AWS_ACCESS_KEY_ID", storageProperties.getS3Config().getAccessKey());
        initContainerEnvs.put("AWS_SECRET_ACCESS_KEY", storageProperties.getS3Config().getSecretKey());
        initContainerEnvs.put("AWS_S3_REGION", storageProperties.getS3Config().getRegion());
        initContainerEnvs.put("SW_PYPI_INDEX_URL",runTimeProperties.getPypi().getIndexUrl());
        initContainerEnvs.put("SW_PYPI_EXTRA_INDEX_URL",runTimeProperties.getPypi().getExtraIndexUrl());
        initContainerEnvs.put("SW_PYPI_TRUSTED_HOST",runTimeProperties.getPypi().getTrustedHost());
        // task container envs
        Map<String, String> coreContainerEnvs = new HashMap<>();
        coreContainerEnvs.put("SW_TASK_STEP", task.getStep().getName());
        // TODO: support multi dataset uris
        // oss env
        List<SWDataSet> swDataSets = swJob.getSwDataSets();
        SWDataSet swDataSet = swDataSets.get(0);
        coreContainerEnvs.put("SW_DATASET_URI", String.format(FORMATTER_URI_DATASET,instanceUri,swJob.getProject().getName(),swDataSet.getName(),swDataSet.getVersion()));
        coreContainerEnvs.put("SW_TASK_INDEX", String.valueOf(task.getTaskRequest().getIndex()));
        coreContainerEnvs.put("SW_EVALUATION_VERSION", swJob.getId().toString());

        swDataSets.forEach(ds -> ds.getFileStorageEnvs().values()
            .forEach(fileStorageEnv -> coreContainerEnvs.putAll(fileStorageEnv.getEnvs())));

        coreContainerEnvs.put(FileStorageEnv.ENV_KEY_PREFIX, swDataSet.getPath());
//        coreContainerEnvs.put("SW_S3_READ_TIMEOUT", );
//        coreContainerEnvs.put("SW_S3_TOTAL_MAX_ATTEMPTS", );

        // datastore env
        coreContainerEnvs.put("SW_TOKEN", jobTokenConfig.getToken());
        coreContainerEnvs.put("SW_INSTANCE_URI", instanceUri);
        coreContainerEnvs.put("SW_PROJECT", swJob.getProject().getName());
        try {
            // cmd（all、single[step、taskIndex]）
            String cmd = "run";
            // TODO: use task's resource needs
            V1ResourceRequirements resourceRequirements = new K8SSelectorSpec(jobRuntime.getDeviceClass(),
                jobRuntime.getDeviceAmount().toString()+"m").getResourceSelector();
            V1Job k8sJob = client.renderJob(getJobTemplate(), task.getId().toString(), "worker", image, List.of(cmd), coreContainerEnvs, initContainerEnvs, resourceRequirements);
            // set result upload path

            log.debug("deploying k8sJob to k8s :{}", JSONUtil.toJsonStr(k8sJob));
            // update node selector
            if (nodeSelector != null) {
                var selector = k8sJob.getSpec().getTemplate().getSpec().getNodeSelector();
                if (selector == null) {
                    selector = nodeSelector;
                } else {
                    selector.putAll(nodeSelector);
                }
                k8sJob.getSpec().getTemplate().getSpec().nodeSelector(selector);
            }
            client.deploy(k8sJob);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getJobTemplate() throws IOException {
        String file = "template/job.yaml";
        InputStream is = this.getClass().getClassLoader()
            .getResourceAsStream(file);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
