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

import ai.starwhale.mlops.configuration.RunTimeProperties;
import ai.starwhale.mlops.configuration.security.TaskTokenValidator;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class OnlineEvaluationTaskSpecificationFactory implements TaskContainerSpecificationFactory {

    public static final String NAME = "online_eval";

    final TaskTokenValidator taskTokenValidator;

    final String instanceUri;

    final RunTimeProperties runTimeProperties;

    public OnlineEvaluationTaskSpecificationFactory(
            TaskTokenValidator taskTokenValidator,
            @Value("${sw.instance-uri}") String instanceUri,
            RunTimeProperties runTimeProperties
    ) {
        this.taskTokenValidator = taskTokenValidator;
        this.instanceUri = instanceUri;
        this.runTimeProperties = runTimeProperties;
    }

    @Override
    public ContainerSpecification containerSpecificationOf(Task task) {
        return new OnlineEvaluationContainerSpecification(task, taskTokenValidator, instanceUri,
                                                          runTimeProperties
        );
    }

    @Override
    public boolean matches(Task task) {
        return NAME.equals(task.getStep().getJob().getVirtualJobName());
    }
}
