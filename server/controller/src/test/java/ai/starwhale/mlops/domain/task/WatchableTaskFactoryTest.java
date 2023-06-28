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

import ai.starwhale.mlops.domain.job.step.task.WatchableTaskFactory;
import ai.starwhale.mlops.domain.job.step.task.bo.Task;
import ai.starwhale.mlops.domain.job.step.task.status.TaskStatus;
import ai.starwhale.mlops.domain.job.step.task.status.watchers.TaskStatusChangeWatcher;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

public class WatchableTaskFactoryTest {

    static List<Integer> list = new LinkedList<>();

    @Test
    public void testWatchableTaskFactory() {

        WatchableTaskFactory watchableTaskFactory = new WatchableTaskFactory(
                List.of(new Watcher2(), new Watcher3(), new Watcher1()));
        Task task = new Task();
        task.updateStatus(TaskStatus.PREPARING);
        Task wrappedTask = watchableTaskFactory.wrapTask(task);
        wrappedTask.updateStatus(TaskStatus.RUNNING);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
    }

    @Order(1)
    static class Watcher1 implements TaskStatusChangeWatcher {

        @Override
        public void onTaskStatusChange(Task task,
                TaskStatus oldStatus) {
            list.add(1);
        }
    }

    @Order(2)
    static class Watcher2 implements TaskStatusChangeWatcher {

        @Override
        public void onTaskStatusChange(Task task,
                TaskStatus oldStatus) {
            list.add(2);
        }
    }

    @Order(3)
    static class Watcher3 implements TaskStatusChangeWatcher {

        @Override
        public void onTaskStatusChange(Task task,
                TaskStatus oldStatus) {
            list.add(3);
        }
    }

}
