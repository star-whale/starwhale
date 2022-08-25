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

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.Task.StatusUnModifiableTask;
import ai.starwhale.mlops.api.protocol.report.resp.TaskRequest;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.WatchableTask;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForCommandingAssurance;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForJobStatus;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForPersist;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForSchedule;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
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
        Agent agent = Agent.builder().build();
        Task oTask = Task.builder()
            .id(10994L)
            .uuid(UUID.randomUUID().toString())
            .status(TaskStatus.CREATED)
            .resultRootPath(resultRootPath)
            .taskRequest(taskRequest)
            .step(Step.builder().build())
            .agent(agent)
            .taskType(TaskType.CMP)
            .build();
        TaskWatcherForSchedule taskWatcherForSchedule = mock(TaskWatcherForSchedule.class);
        TaskWatcherForPersist taskWatcherForPersist = mock(TaskWatcherForPersist.class);
        TaskWatcherForJobStatus taskWatcherForJobStatus = mock(TaskWatcherForJobStatus.class);
        TaskWatcherForCommandingAssurance taskWatcherForCommandingAssurance = mock(TaskWatcherForCommandingAssurance.class);

        List<TaskStatusChangeWatcher> watchers = List.of(taskWatcherForSchedule,taskWatcherForJobStatus,taskWatcherForCommandingAssurance,taskWatcherForPersist);
        WatchableTask watchableTask = new WatchableTask(oTask,watchers,new TaskStatusMachine());

        Method[] methods = oTask.getClass().getMethods();
        Method[] methodsWatchable = watchableTask.getClass().getMethods();
        Map<String, List<Method>> methodMap = Arrays.stream(methodsWatchable)
            .collect(Collectors.groupingBy(Method::getName));
        for(Method method:methods){
            String methodName = method.getName();
            if(methodName.startsWith("get") && !methodName.equals("getClass")){
                log.debug("now checking {}", methodName);
                Assertions.assertTrue(method.invoke(oTask) == methodMap.get(methodName).get(0).invoke(watchableTask));
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
        Agent newAgent = Agent.builder().build();
        watchableTask.setAgent(newAgent);
        ResultPath newResultRootPath = new ResultPath();
        watchableTask.setResultRootPath(newResultRootPath);
        Assertions.assertEquals(TaskStatus.READY,oTask.getStatus());
        verify(taskWatcherForSchedule).onTaskStatusChange(watchableTask,TaskStatus.CREATED);
        verify(taskWatcherForPersist).onTaskStatusChange(watchableTask,TaskStatus.CREATED);
        verify(taskWatcherForJobStatus).onTaskStatusChange(watchableTask,TaskStatus.CREATED);
        verify(taskWatcherForCommandingAssurance).onTaskStatusChange(watchableTask,TaskStatus.CREATED);
        Assertions.assertTrue(oTask.getResultRootPath() == newResultRootPath);
        Assertions.assertTrue(oTask.getResultRootPath() != resultRootPath);

        Assertions.assertTrue(oTask.getAgent() == newAgent);
        Assertions.assertTrue(oTask.getAgent() != agent);

        Assertions.assertTrue(oTask.getTaskRequest() == newTaskRequest);
        Assertions.assertTrue(oTask.getTaskRequest() != taskRequest);

        TaskStatusChangeWatcher.SKIPPED_WATCHERS.set(Set.of(taskWatcherForPersist.getClass()));
        watchableTask.updateStatus(TaskStatus.RUNNING);
        verify(taskWatcherForSchedule,times(1)).onTaskStatusChange(watchableTask,TaskStatus.READY);
        verify(taskWatcherForPersist,times(0)).onTaskStatusChange(watchableTask,TaskStatus.READY);
        verify(taskWatcherForJobStatus,times(1)).onTaskStatusChange(watchableTask,TaskStatus.READY);
        verify(taskWatcherForCommandingAssurance,times(1)).onTaskStatusChange(watchableTask,TaskStatus.READY);
        TaskStatusChangeWatcher.SKIPPED_WATCHERS.remove();
    }



    @Test
    public void testUnwrap() throws NoSuchFieldException, IllegalAccessException {
        Task oTask = Task.builder().build();
        Task.StatusUnModifiableTask unModifiableTask = new StatusUnModifiableTask(oTask);
        WatchableTask watchableTask1 = new WatchableTask(unModifiableTask,null,null);
        WatchableTask watchableTask2 = new WatchableTask(oTask,null,null);
        WatchableTask watchableTask3 = new WatchableTask(watchableTask2,null,null);
        Assertions.assertTrue(watchableTask1.unwrap() == oTask);
        Assertions.assertTrue(watchableTask2.unwrap() == oTask);
        Assertions.assertTrue(watchableTask3.unwrap() == oTask);
        Field declaredField = watchableTask1.getClass().getDeclaredField("oTask");
        declaredField.setAccessible(true);
        Assertions.assertTrue(declaredField.get(watchableTask1) == oTask);
        Assertions.assertTrue(declaredField.get(watchableTask2) == oTask);
        Assertions.assertTrue(declaredField.get(watchableTask3) == oTask);
    }

}
