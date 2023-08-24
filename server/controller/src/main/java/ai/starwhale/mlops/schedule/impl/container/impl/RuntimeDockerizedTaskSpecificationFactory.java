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

import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Component
@Order(3)
public class RuntimeDockerizedTaskSpecificationFactory implements TaskContainerSpecificationFactory {

    public static final String NAME = "runtime_dockerizing";

    final String instanceUri;

    final SystemSettingService systemSettingService;
    final TaskTokenValidator taskTokenValidator;

    final RuntimeVersionMapper runtimeVersionMapper;

    public RuntimeDockerizedTaskSpecificationFactory(
            @Value("${sw.instance-uri}") String instanceUri,
            SystemSettingService systemSettingService,
            TaskTokenValidator taskTokenValidator,
            RuntimeVersionMapper runtimeVersionMapper
    ) {
        this.instanceUri = instanceUri;
        this.systemSettingService = systemSettingService;
        this.taskTokenValidator = taskTokenValidator;
        this.runtimeVersionMapper = runtimeVersionMapper;
    }

    @Override
    public ContainerSpecification containerSpecificationOf(Task task) {
        return new RuntimeDockerizedContainerSpecification(
                task,
                instanceUri,
                systemSettingService,
                taskTokenValidator,
                runtimeVersionMapper
        );
    }

    @Override
    public boolean matches(Task task) {
        return NAME.equals(task.getStep().getJob().getVirtualJobName());
    }
}
