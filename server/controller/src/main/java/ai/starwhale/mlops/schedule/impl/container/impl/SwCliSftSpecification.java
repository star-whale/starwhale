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

package ai.starwhale.mlops.schedule.impl.container.impl;

import ai.starwhale.mlops.common.proxy.WebServerInTask;
import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.sft.po.SftEntity;
import ai.starwhale.mlops.domain.sft.po.SftMapper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

@Slf4j
public class SwCliSftSpecification extends SwCliModelHandlerContainerSpecification {


    private final SftMapper sftMapper;

    private final DatasetDao datasetDao;

    public SwCliSftSpecification(
            @Value("${sw.instance-uri}") String instanceUri,
            @Value("${sw.task.dev-port}") int devPort,
            @Value("${sw.dataset.load.batch-size}") int datasetLoadBatchSize,
            RunTimeProperties runTimeProperties,
            TaskTokenValidator taskTokenValidator,
            WebServerInTask webServerInTask,
            Task task,
            SftMapper sftMapper,
            DatasetDao datasetDao
    ) {
        super(instanceUri, devPort, datasetLoadBatchSize, runTimeProperties, taskTokenValidator, webServerInTask, task);
        this.sftMapper = sftMapper;
        this.datasetDao = datasetDao;
    }



    public Map<String, String> getContainerEnvs() {
        Map<String, String> containerEnvs = super.getContainerEnvs();
        SftEntity sft = sftMapper.findSftByJob(task.getStep().getJob().getId());
        if(!CollectionUtils.isEmpty(sft.getEvalDatasets())){
            String evalDataSetUris = sft.getEvalDatasets().stream().map(dsv -> {
                DatasetVersion datasetVersion = datasetDao.getDatasetVersion(dsv);
                return String.format(
                        ContainerSpecification.FORMATTER_URI_ARTIFACT,
                        instanceUri,
                        datasetVersion.getProjectId(),
                        "dataset",
                        datasetVersion.getDatasetName(),
                        datasetVersion.getVersionName()
                );

            }).collect(Collectors.joining(" "));
            containerEnvs.put("SW_FT_VALIDATION_DATASETS", evalDataSetUris);
        }
        containerEnvs.put("SFTID", String.valueOf(sft.getId()));
        return containerEnvs;
    }
}
