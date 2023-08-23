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

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import java.util.Map;


public class OnlineEvaluationContainerSpecification implements ContainerSpecification {

    final Task task;

    public OnlineEvaluationContainerSpecification(Task task) {
        this.task = task;
    }

    @Override
    public Map<String, String> getContainerEnvs() {
        return null;
    }

    @Override
    public ContainerCommand getCmd() {
        return ContainerCommand.builder().cmd(new String[]{"serve"}).build();
    }

    @Override
    public String getImage() {
        return null;
    }


}
