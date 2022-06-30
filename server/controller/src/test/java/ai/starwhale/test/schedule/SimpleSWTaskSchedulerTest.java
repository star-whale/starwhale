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

package ai.starwhale.test.schedule;

import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.node.Device.Status;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.SimpleSWTaskScheduler;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

/**
 * Test for {@link ai.starwhale.mlops.schedule.SimpleSWTaskScheduler}
 */
public class SimpleSWTaskSchedulerTest {



    @Test
    public void testNormal() {
        AtomicLong atomicLong = new AtomicLong(0);
        SimpleSWTaskScheduler simpleSWTaskScheduler = new SimpleSWTaskScheduler();
        int CPU_TASK_SIZE = 117;
        int GPU_TASK_SIZE = 121;
        simpleSWTaskScheduler.adoptTasks(mockTask(CPU_TASK_SIZE,atomicLong), Clazz.CPU);
        simpleSWTaskScheduler.adoptTasks(mockTask(GPU_TASK_SIZE,atomicLong), Clazz.GPU);
        Random random = new Random();
        while (true) {
            int idleCPU = random.nextInt(5);
            int idleGPU = random.nextInt(9);
            List<Task> sheduledTasks = simpleSWTaskScheduler.schedule(mockNode(idleCPU, idleGPU));
            int realCPU = CPU_TASK_SIZE < idleCPU ? CPU_TASK_SIZE : idleCPU;
            int realGPU = GPU_TASK_SIZE < idleGPU ? GPU_TASK_SIZE : idleGPU;
            Assertions.assertEquals(realCPU + realGPU, sheduledTasks.size());
            CPU_TASK_SIZE = CPU_TASK_SIZE < idleCPU ? 0 : CPU_TASK_SIZE - idleCPU;
            GPU_TASK_SIZE = GPU_TASK_SIZE < idleGPU ? 0 : GPU_TASK_SIZE - idleGPU;
            if (CPU_TASK_SIZE == 0 && GPU_TASK_SIZE == 0) {
                break;
            }

        }


    }

    @Test
    public void testStopAndREAdopt() {
        AtomicLong atomicLong = new AtomicLong(0);
        SimpleSWTaskScheduler simpleSWTaskScheduler = new SimpleSWTaskScheduler();
        int CPU_TASK_SIZE = 117;
        int GPU_TASK_SIZE = 121;
        Set<Task> CPU_TASKS = mockTask(CPU_TASK_SIZE,atomicLong);
        Set<Task> GPU_TASKS = mockTask(GPU_TASK_SIZE,atomicLong);
        simpleSWTaskScheduler.adoptTasks(CPU_TASKS, Clazz.CPU);
        simpleSWTaskScheduler.adoptTasks(GPU_TASKS, Clazz.GPU);
        long currentTask = atomicLong.get();
        Random random = new Random();
        Set<Long> stoppedTaskIds = new HashSet<>();
        for (int i = 0; i < currentTask / 4; i++) {
            stoppedTaskIds.add(
                Long.valueOf(random.nextInt(Long.valueOf(currentTask).intValue())) + 1);
        }
        simpleSWTaskScheduler.stopSchedule(stoppedTaskIds);
        Set<Long> finalScheduledTasks = new HashSet<>();
        doSchedule(random, simpleSWTaskScheduler, finalScheduledTasks);
        stoppedTaskIds.forEach(t -> {
            Assertions.assertTrue(!finalScheduledTasks.contains(t));
        });
        Assertions.assertEquals(CPU_TASK_SIZE + GPU_TASK_SIZE,
            stoppedTaskIds.size() + finalScheduledTasks.size());

        Map<Long, List<Task>> idTasksCPU = CPU_TASKS.parallelStream()
            .collect(Collectors.groupingBy(Task::getId));
        Map<Long, List<Task>> idTasksGPU = GPU_TASKS.parallelStream()
            .collect(Collectors.groupingBy(Task::getId));
        List<Task> stoppedCPUTaskList = stoppedTaskIds.parallelStream()
            .map(tid -> idTasksCPU.get(tid)).filter(
                Objects::nonNull).flatMap(
                Collection::stream).collect(Collectors.toList());
        List<Task> stoppedGPUTaskList = stoppedTaskIds.parallelStream()
            .map(tid -> idTasksGPU.get(tid)).filter(
                Objects::nonNull).flatMap(
                Collection::stream).collect(Collectors.toList());
        simpleSWTaskScheduler.adoptTasks(stoppedCPUTaskList, Clazz.CPU);
        simpleSWTaskScheduler.adoptTasks(stoppedGPUTaskList, Clazz.GPU);

        Set<Long> secondScheduledTasks = new HashSet<>();
        doSchedule(random, simpleSWTaskScheduler, secondScheduledTasks);
        Assertions.assertEquals(CPU_TASK_SIZE + GPU_TASK_SIZE, finalScheduledTasks.size() + secondScheduledTasks.size());
    }

    private void doSchedule(Random random, SimpleSWTaskScheduler simpleSWTaskScheduler,
        Set<Long> finalScheduledTasks) {
        while (true) {
            int idleCPU = random.nextInt(4) +1 ;
            int idleGPU = random.nextInt(8) + 1;
            List<Task> onceScheduledTasks = simpleSWTaskScheduler.schedule(
                mockNode(idleCPU, idleGPU));
            if (CollectionUtils.isEmpty(onceScheduledTasks)) {
                break;
            }
            finalScheduledTasks.addAll(onceScheduledTasks.parallelStream()
                .map(Task::getId)
                .collect(Collectors.toList()));
        }
    }

    /**
     * @param idleCPU <= 4
     * @param idleGPU <= 8
     * @return
     */
    Node mockNode(int idleCPU, int idleGPU) {
        return Node.builder().serialNumber(UUID.randomUUID().toString())
            .ipAddr("120.0.9.8")
            .devices(mockDevices(idleCPU, idleGPU))
            .build();
    }

    private List<Device> mockDevices(int idleCPU, int idleGPU) {
        List<Device> mockedDevices = new LinkedList<>();
        mockOneType(mockedDevices, Clazz.CPU, 4, idleCPU);
        mockOneType(mockedDevices, Clazz.GPU, 8, idleGPU);
        return mockedDevices;
    }

    private void mockOneType(List<Device> mockedDevices, Device.Clazz clazz, int deviceCount,
        int idleCount) {
        for (int i = 0; i < deviceCount; i++) {
            mockedDevices.add(mockDevice(clazz, i < idleCount ? Status.idle : Status.busy));
        }
    }

    private Device mockDevice(Device.Clazz deviceClass, Status deviceStatus) {
        return Device.builder().clazz(deviceClass)
            .status(deviceStatus)
            .build();
    }

    Set<Task> mockTask(int size,AtomicLong atomicLong) {
        Set<Task> tasks = new HashSet<>();
        for (int i = 0; i < size; i++) {
            tasks.add(
                Task.builder().id(atomicLong.incrementAndGet()).uuid(UUID.randomUUID().toString())
                    .build());
        }
        return tasks;
    }
}
