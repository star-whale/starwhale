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

package ai.starwhale.mlops.domain.task.status;

import ai.starwhale.mlops.domain.task.bo.Task;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
public class WatchableTaskFactory {

    final List<TaskStatusChangeWatcher> taskStatusChangeWatchers;

    final TaskStatusMachine taskStatusMachine;


    public WatchableTaskFactory(
        List<TaskStatusChangeWatcher> taskStatusChangeWatchers,
        TaskStatusMachine taskStatusMachine) {
        this.taskStatusChangeWatchers = taskStatusChangeWatchers.stream().sorted((w1,w2)->{
            Order o1 = w1.getClass().getAnnotation(Order.class);
            if(null == o1){
                return -1;
            }
            Order o2 = w2.getClass().getAnnotation(Order.class);
            if(null == o2){
                return 1;
            }
            return o1.value() - o2.value();
        }).collect(Collectors.toList());
        this.taskStatusMachine = taskStatusMachine;
    }

    public Task wrapTask(Task task){
        return new WatchableTask(task,taskStatusChangeWatchers,taskStatusMachine);
    }

    public List<Task> wrapTasks(Collection<Task> tasks){
        return tasks.parallelStream()
            .map(task->new WatchableTask(task,taskStatusChangeWatchers,taskStatusMachine))
            .collect(Collectors.toList());
    }
}
