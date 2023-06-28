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

package ai.starwhale.mlops.domain.task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.task.WatchableTask;
import ai.starwhale.mlops.domain.job.step.task.bo.ResultPath;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.bo.TaskRequest;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskWatcherForJobStatus;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskWatcherForPersist;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskWatcherForSchedule;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link WatchableTask}
 */
@Slf4j
public class WatchableTaskTest {


    @Test
    public void testWatchableTask() throws InvocationTargetException, IllegalAccessException {
        TaskRequest taskRequest = new TaskRequest() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        };
        ResultPath resultRootPath = new ResultPath("");
        Task originalTask = Task.builder()
                .id(10994L)
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.CREATED)
                .resultRootPath(resultRootPath)
                .taskRequest(taskRequest)
                .step(Step.builder().build())
                .build();
        TaskWatcherForSchedule taskWatcherForSchedule = mock(TaskWatcherForSchedule.class);
        TaskWatcherForPersist taskWatcherForPersist = mock(TaskWatcherForPersist.class);
        TaskWatcherForJobStatus taskWatcherForJobStatus = mock(TaskWatcherForJobStatus.class);

        List<TaskStatusChangeWatcher> watchers = List.of(taskWatcherForSchedule, taskWatcherForJobStatus,
                taskWatcherForPersist);
        WatchableTask watchableTask = new WatchableTask(originalTask, watchers);

        Method[] methods = originalTask.getClass().getMethods();
        Method[] methodsWatchable = watchableTask.getClass().getMethods();
        Map<String, List<Method>> methodMap = Arrays.stream(methodsWatchable)
                .collect(Collectors.groupingBy(Method::getName));
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && !methodName.equals("getClass")) {
                log.debug("now checking {}", methodName);
                Assertions.assertEquals(method.invoke(originalTask),
                        methodMap.get(methodName).get(0).invoke(watchableTask));
            }
        }

        watchableTask.updateStatus(TaskStatus.READY);
        TaskRequest newTaskRequest = new TaskRequest() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        };
        watchableTask.setTaskRequest(newTaskRequest);
        ResultPath newResultRootPath = new ResultPath();
        watchableTask.setResultRootPath(newResultRootPath);
        Assertions.assertEquals(TaskStatus.READY, originalTask.getStatus());
        verify(taskWatcherForSchedule).onTaskStatusChange(watchableTask, TaskStatus.CREATED);
        verify(taskWatcherForPersist).onTaskStatusChange(watchableTask, TaskStatus.CREATED);
        verify(taskWatcherForJobStatus).onTaskStatusChange(watchableTask, TaskStatus.CREATED);
        Assertions.assertTrue(originalTask.getResultRootPath() == newResultRootPath);
        Assertions.assertTrue(originalTask.getResultRootPath() != resultRootPath);

        Assertions.assertTrue(originalTask.getTaskRequest() == newTaskRequest);
        Assertions.assertTrue(originalTask.getTaskRequest() != taskRequest);

        TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(Set.of(taskWatcherForPersist.getClass()));
        watchableTask.updateStatus(TaskStatus.RUNNING);
        verify(taskWatcherForSchedule, times(1)).onTaskStatusChange(watchableTask, TaskStatus.READY);
        verify(taskWatcherForPersist, times(0)).onTaskStatusChange(watchableTask, TaskStatus.READY);
        verify(taskWatcherForJobStatus, times(1)).onTaskStatusChange(watchableTask, TaskStatus.READY);
        TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
    }


    @Test
    public void testUnwrap() throws NoSuchFieldException, IllegalAccessException {
        Task originalTask = Task.builder().build();
        WatchableTask unModifiableTask = new WatchableTask(originalTask, Collections.emptyList());
        WatchableTask watchableTask1 = new WatchableTask(unModifiableTask, null);
        WatchableTask watchableTask2 = new WatchableTask(originalTask, null);
        WatchableTask watchableTask3 = new WatchableTask(watchableTask2, null);
        Assertions.assertTrue(watchableTask1.unwrap() == originalTask);
        Assertions.assertTrue(watchableTask2.unwrap() == originalTask);
        Assertions.assertTrue(watchableTask3.unwrap() == originalTask);
        Field declaredField = watchableTask1.getClass().getDeclaredField("originalTask");
        declaredField.setAccessible(true);
        Assertions.assertTrue(declaredField.get(watchableTask1) == originalTask);
        Assertions.assertTrue(declaredField.get(watchableTask2) == originalTask);
        Assertions.assertTrue(declaredField.get(watchableTask3) == originalTask);
    }

    @Test
    public void testCancelFromPreparing() {
        var originalTask = Task.builder()
                .uuid(UUID.randomUUID().toString())
                .status(TaskStatus.PREPARING).build();
        var taskWatcherForSchedule = mock(TaskWatcherForSchedule.class);
        var taskWatcherForPersist = mock(TaskWatcherForPersist.class);
        var taskWatcherForJobStatus = mock(TaskWatcherForJobStatus.class);

        var watchers = List.of(taskWatcherForSchedule, taskWatcherForJobStatus, taskWatcherForPersist);
        WatchableTask watchableTask = new WatchableTask(originalTask, watchers);
        watchableTask.updateStatus(TaskStatus.CANCELED);
        Assertions.assertEquals(TaskStatus.CANCELED, originalTask.getStatus());
        verify(taskWatcherForSchedule).onTaskStatusChange(watchableTask, TaskStatus.PREPARING);
        verify(taskWatcherForPersist).onTaskStatusChange(watchableTask, TaskStatus.PREPARING);
        verify(taskWatcherForJobStatus).onTaskStatusChange(watchableTask, TaskStatus.PREPARING);
    }
}
