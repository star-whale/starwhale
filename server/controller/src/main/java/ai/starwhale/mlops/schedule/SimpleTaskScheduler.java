/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.node.Device.Status;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * a simple implementation of JobScheduler
 */
@Slf4j
public class SimpleTaskScheduler implements TaskScheduler {

    final Map<Device.Clazz, ConcurrentLinkedQueue<TaskTrigger>> taskQueueTable;

    public SimpleTaskScheduler() {
        this.taskQueueTable = Map.of(Clazz.CPU, new ConcurrentLinkedQueue<>(),
            Clazz.GPU, new ConcurrentLinkedQueue<>());
        loadTasks();
    }

    //TODO load tasks that is new created
    private void loadTasks() {
    }

    @Override
    public void adoptTasks(Collection<TaskTrigger> TaskTriggers, Device.Clazz deviceClass) {
        taskQueueTable.get(deviceClass).addAll(TaskTriggers);
    }

    @Override
    public List<TaskTrigger> schedule(Node node) {
        validNode(node);
        return node.getDevices()
            .stream()
            .filter(device -> Status.idle == device.getStatus()) //only schedule devices that is free
            .map(device -> taskQueueTable.get(device.getClazz()).poll())// pull task from the device corresponding queue
            .filter(Objects::nonNull)//remove null tasks got from empty queue
            .collect(Collectors.toList());
    }

    private void validNode(Node node) {
        //assuming that all DeviceHolders are valid
        if (null == node || null == node.getDevices()) {
            log.error("bad node scheduled, null or null field");
            throw new SWValidationException(ValidSubject.NODE);
        }
    }

}
