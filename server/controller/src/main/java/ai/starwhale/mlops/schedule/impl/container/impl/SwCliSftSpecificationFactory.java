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
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.sft.mapper.SftMapper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(98)
@Component
public class SwCliSftSpecificationFactory extends SwCliModelHandlerSpecificationFactory {

    final String instanceUri;
    final int devPort;
    final int datasetLoadBatchSize;
    final RunTimeProperties runTimeProperties;
    final TaskTokenValidator taskTokenValidator;

    private final WebServerInTask webServerInTask;

    private final SftMapper sftMapper;

    private final DatasetDao datasetDao;


    public SwCliSftSpecificationFactory(
            @Value("${sw.instance-uri}") String instanceUri,
            @Value("${sw.task.dev-port}") int devPort,
            @Value("${sw.dataset.load.batch-size}") int datasetLoadBatchSize,
            RunTimeProperties runTimeProperties,
            TaskTokenValidator taskTokenValidator,
            WebServerInTask webServerInTask,
            SftMapper sftMapper,
            DatasetDao datasetDao
    ) {
        super(instanceUri, devPort, datasetLoadBatchSize, runTimeProperties, taskTokenValidator, webServerInTask);
        this.instanceUri = instanceUri;
        this.devPort = devPort;
        this.datasetLoadBatchSize = datasetLoadBatchSize;
        this.runTimeProperties = runTimeProperties;
        this.taskTokenValidator = taskTokenValidator;
        this.webServerInTask = webServerInTask;
        this.sftMapper = sftMapper;
        this.datasetDao = datasetDao;
    }

    @Override
    public ContainerSpecification containerSpecificationOf(Task task) {
        return new SwCliSftSpecification(
                instanceUri,
                devPort,
                datasetLoadBatchSize,
                runTimeProperties,
                taskTokenValidator,
                webServerInTask,
                task,
                sftMapper,
                datasetDao
        );
    }

    @Override
    public boolean matches(Task task) {
        return JobType.FINE_TUNE == task.getStep().getJob().getType();
    }
}
