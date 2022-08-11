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

import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.converter.TaskBoConverter;
import ai.starwhale.mlops.schedule.SWTaskScheduler;
import ai.starwhale.mlops.storage.configuration.StorageProperties;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1EnvVar;
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
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class K8sTaskScheduler implements SWTaskScheduler {

    final K8sClient k8sClient;

    final TaskBoConverter taskConvertor;

    final StorageProperties storageProperties;

    final RunTimeProperties runTimeProperties;

    public K8sTaskScheduler(K8sClient k8sClient,
        TaskBoConverter taskConvertor, StorageProperties storageProperties,
        RunTimeProperties runTimeProperties) {
        this.k8sClient = k8sClient;
        this.taskConvertor = taskConvertor;
        this.storageProperties = storageProperties;
        this.runTimeProperties = runTimeProperties;
    }

    @Override
    public void adopt(Collection<Task> tasks,
                           Clazz deviceClass) {

        tasks.parallelStream().forEach(task -> {
            this.deployTaskToK8s(k8sClient,task.getStep().getJob().getJobRuntime().getImage(),taskConvertor.toTaskTrigger(task));
        });
    }

    @Override
    public void remove(Collection<Long> taskIds) {
        taskIds.parallelStream().forEach(id->{
            try {
                k8sClient.deleteJob(id.toString());
            } catch (ApiException e) {
                log.warn("delete k8s job failed {}",id,e);
            }
        });
    }

    /**
     * todo hard code in this piece of code will be refactored after other core concepts being refactored
     * @param client
     * @param image
     * @param task
     */
    private void deployTaskToK8s(K8sClient client, String image, TaskTrigger task) {
        log.debug("deploying task to k8s {} {} {}", task.getId(), task.getResultPath(), task.getTaskType());
        Map<String, String> envs = new HashMap<>();
        List<String> downloads = new ArrayList<>();
        String prefix = "s3://"+storageProperties.getS3Config().getBucket()+"/";
        downloads.add(prefix + task.getSwModelPackage().getPath()+";/opt/starwhale/swmp/");
        downloads.add(prefix + task.getSwrt().getPath()+";/opt/starwhale/swrt/");
        envs.put("DOWNLOADS", Strings.join(downloads, ' '));
        String input = generateConfigFile(task);
        envs.put("INPUT", input);
        envs.put("ENDPOINT_URL",storageProperties.getS3Config().getEndpoint());
        envs.put("AWS_ACCESS_KEY_ID",storageProperties.getS3Config().getAccessKey());
        envs.put("AWS_SECRET_ACCESS_KEY",storageProperties.getS3Config().getSecretKey());
        envs.put("AWS_S3_REGION",storageProperties.getS3Config().getRegion());
        envs.put("SW_PYPI_INDEX_URL",runTimeProperties.getPypi().getIndexUrl());
        envs.put("SW_PYPI_EXTRA_INDEX_URL",runTimeProperties.getPypi().getExtraIndexUrl());
        envs.put("SW_PYPI_TRUSTED_HOST",runTimeProperties.getPypi().getTrustedHost());
        envs.put("SW_PIP_CACHE_DIR","/root/.cache/pip");
        try {
            String cmd = "ppl";
            if (task.getTaskType() == TaskType.CMP) {
                cmd = "cmp";
            }
            V1ResourceRequirements resourceRequirements = new K8SSelectorSpec(task.getDeviceClass(),
                task.getDeviceAmount().toString()).getResourceSelector();
            V1Job job = client.renderJob(getJobTemplate(), task.getId().toString(), "worker", image, List.of(cmd), envs,resourceRequirements);
            // set result upload path
            job.getSpec().getTemplate().getSpec().getContainers().get(0).env(List.of(new V1EnvVar().name("DST").value(prefix+ task.getResultPath().resultDir())
                ,new V1EnvVar().name("ENDPOINT_URL").value(storageProperties.getS3Config().getEndpoint())
                ,new V1EnvVar().name("AWS_ACCESS_KEY_ID").value(storageProperties.getS3Config().getAccessKey())
                ,new V1EnvVar().name("AWS_S3_REGION").value(storageProperties.getS3Config().getRegion())
                ,new V1EnvVar().name("AWS_SECRET_ACCESS_KEY").value(storageProperties.getS3Config().getSecretKey())
                ,new V1EnvVar().name("SW_PYPI_INDEX_URL").value(runTimeProperties.getPypi().getIndexUrl())
                ,new V1EnvVar().name("SW_PYPI_EXTRA_INDEX_URL").value(runTimeProperties.getPypi().getExtraIndexUrl())
                ,new V1EnvVar().name("SW_PYPI_TRUSTED_HOST").value(runTimeProperties.getPypi().getTrustedHost())
                )
            );
            client.deploy(job);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateConfigFile(TaskTrigger task) {
        JSONObject object = JSONUtil.createObj();
        object.set("backend", "s3");
        object.set("secret", JSONUtil.createObj().set("access_key", storageProperties.getS3Config().getAccessKey()).set("secret_key", storageProperties.getS3Config().getSecretKey()));
        object.set("service", JSONUtil.createObj()
            .set("endpoint", storageProperties.getS3Config().getEndpoint())
            .set("region", storageProperties.getS3Config().getRegion())
        );
        final String dataFormat = "%s:%s:%s";
        switch (task.getTaskType()) {
            case PPL:
                object.set("kind", "swds");
                JSONArray swds = JSONUtil.createArray();

                task.getSwdsBlocks().forEach(swdsBlock -> {
                    JSONObject ds = JSONUtil.createObj();
                    ds.set("bucket", storageProperties.getS3Config().getBucket());
                    ds.set("key", JSONUtil.createObj()
                        .set("data", String.format(dataFormat, swdsBlock.getLocationInput().getFile(), swdsBlock.getLocationInput().getOffset(), swdsBlock.getLocationInput().getOffset() + swdsBlock.getLocationInput().getSize() - 1))
                        .set("label", String.format(dataFormat, swdsBlock.getLocationLabel().getFile(), swdsBlock.getLocationLabel().getOffset(), swdsBlock.getLocationLabel().getOffset() + swdsBlock.getLocationLabel().getSize() - 1))
                    );
                    ds.set("ext_attr", JSONUtil.createObj()
                        .set("swds_name", swdsBlock.getDsName())
                        .set("swds_version", swdsBlock.getDsVersion())
                    );
                    swds.add(ds);
                });
                object.set("swds", swds);
                break;
            case CMP:
                object.set("kind", "jsonl");
                JSONArray cmp = JSONUtil.createArray();
                task.getCmpInputFilePaths().forEach(inputFilePath -> {
                    JSONObject ds = JSONUtil.createObj();
                    ds.set("bucket", storageProperties.getS3Config().getBucket());
                    ds.set("key", JSONUtil.createObj()
                        .set("data", inputFilePath)
                    );
                    cmp.add(ds);
                });

                object.set("swds", cmp);
        }

        return JSONUtil.toJsonStr(object);
    }

    private String getJobTemplate() throws IOException {
        String file ="template/job.yaml";
        InputStream is = this.getClass().getClassLoader()
            .getResourceAsStream(file);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
