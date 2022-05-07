/**
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

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.node.Device.Status;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import cn.hutool.core.collection.ConcurrentHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * a simple implementation of JobScheduler
 */
@Slf4j
@Service
public class SimpleSWTaskScheduler implements SWTaskScheduler {

    final Map<Device.Clazz, ConcurrentLinkedQueue<Task>> taskQueueTable;

    final List<Long> stoppedTaskIds = Collections.synchronizedList(new LinkedList<>());

    public SimpleSWTaskScheduler() {
        this.taskQueueTable = Map.of(Clazz.CPU, new ConcurrentLinkedQueue<>(),
            Clazz.GPU, new ConcurrentLinkedQueue<>());
    }

    @Override
    public void adoptTasks(Collection<Task> tasks, Device.Clazz deviceClass) {
        taskQueueTable.get(deviceClass).addAll(tasks);
    }

    @Override
    public void stopSchedule(Collection<Long> taskIds) {
        stoppedTaskIds.addAll(taskIds);
    }

    @Override
    public List<Task> schedule(Node node) {
        validNode(node);
        return node.getDevices()
            .stream()
            .filter(device -> Status.idle == device.getStatus()) //only schedule devices that is free
            .map(this::pollTask)// pull task from the device corresponding queue
            .filter(Objects::nonNull)//remove null tasks got from empty queue
            .collect(Collectors.toList());
    }

    private Task pollTask(Device device) {
        ConcurrentLinkedQueue<Task> taskQueue = taskQueueTable.get(device.getClazz());
        Task tobeScheduledTask;
        do{
            tobeScheduledTask = taskQueue.poll();
            if(tobeScheduledTask == null){
                return null;
            }

            Long taskId = tobeScheduledTask.getId();
            if(stoppedTaskIds.contains(taskId)){
                stoppedTaskIds.remove(taskId);
                continue;
            }
            return tobeScheduledTask;
        }while (true);

    }

    private void validNode(Node node) {
        //assuming that all DeviceHolders are valid
        if (null == node || null == node.getDevices()) {
            log.error("bad node scheduled, null or null field");
            throw new SWValidationException(ValidSubject.NODE);
        }
    }

}
