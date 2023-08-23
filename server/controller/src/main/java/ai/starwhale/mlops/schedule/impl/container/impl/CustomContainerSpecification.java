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

import ai.starwhale.mlops.domain.job.spec.ContainerSpec;
import ai.starwhale.mlops.domain.job.spec.Env;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;


public class CustomContainerSpecification implements ContainerSpecification {

    final ContainerSpec containerSpec;

    final StepSpec spec;

    public CustomContainerSpecification(Task task) {
        spec = task.getStep().getSpec();
        if (null == spec || null == spec.getContainerSpec()) {
            throw new SwValidationException(ValidSubject.TASK,
                    "task is expected to have custom step spec when building the entrypoint");
        }
        this.containerSpec = spec.getContainerSpec();
    }

    @Override
    public Map<String, String> getContainerEnvs() {
        if (CollectionUtils.isEmpty(spec.getEnv())) {
            return Map.of();
        }
        return spec.getEnv().stream().collect(Collectors.toMap(Env::getName, Env::getValue));
    }

    @Override
    public ContainerCommand getCmd() {
        return new ContainerCommand(containerSpec.getCmds(), containerSpec.getEntrypoint());
    }

    @Override
    public String getImage() {
        return containerSpec.getImage();
    }


}
