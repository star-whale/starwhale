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

package ai.starwhale.mlops.schedule.impl.docker;

import ai.starwhale.mlops.domain.task.bo.Task;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.model.Container;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class ContainerTaskMapper {

    static final String CONTAINER_LABEL_TASK_ID = "starwhale-task-id";
    public static final String CONTAINER_LABEL_GENERATION = "starwhale-generation";

    final DockerClientFinder dockerClientFinder;

    public ContainerTaskMapper(DockerClientFinder dockerClientFinder) {
        this.dockerClientFinder = dockerClientFinder;
    }

    public String containerName(Task task) {
        return String.format("starwhale-task-%d-%d", task.getId(), System.currentTimeMillis());
    }

    public Container containerOfTask(Task task) {
        List<Container> containers = dockerClientFinder.findProperDockerClient(task.getStep().getResourcePool())
                .listContainersCmd().withShowAll(true)
                .withLabelFilter(Map.of(CONTAINER_LABEL_TASK_ID, task.getId().toString())).exec();
        if (CollectionUtils.isEmpty(containers)) {
            return null;
        }
        if (containers.size() > 1) {
            log.warn("multiple containers found for task {}", task.getId());
        }
        return containers.get(0);
    }

    public Long taskIfOfContainer(Container container) {
        String taskId = container.getLabels().get(CONTAINER_LABEL_TASK_ID);
        if (null != taskId && StrUtil.isNumeric(taskId)) {
            return Long.valueOf(taskId);
        }
        return null;
    }

}
