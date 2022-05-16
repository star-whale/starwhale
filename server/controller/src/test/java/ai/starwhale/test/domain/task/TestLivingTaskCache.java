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

package ai.starwhale.test.domain.task;

import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobRuntime;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.cache.CacheWrapperForWatch;
import ai.starwhale.mlops.domain.task.cache.LivingTaskCacheImpl;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.cmp.CMPRequest;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.TaskStatusChangeWatcher;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.domain.task.status.watchers.TaskWatcherForCache;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestLivingTaskCache {

    TaskStatusMachine taskStatusMachine = new TaskStatusMachine();

    TaskMapper taskMapper = new TaskMapper() {
        @Override
        public List<TaskEntity> listTasks(Long jobId) {
            return null;
        }

        @Override
        public TaskEntity findTaskById(Long taskId) {
            return null;
        }

        @Override
        public int addTask(TaskEntity task) {
            return 0;
        }

        @Override
        public int addAll(List<TaskEntity> taskList) {
            return 0;
        }

        @Override
        public void updateTaskStatus(List<Long> taskIds, TaskStatus taskStatus) {

        }

        @Override
        public List<TaskEntity> findTaskByStatus(TaskStatus taskStatus) {
            return null;
        }

        @Override
        public List<TaskEntity> findTaskByStatusIn(List<TaskStatus> taskStatusList) {
            return null;
        }

        @Override
        public void updateTaskAgent(List<Long> taskIds, Long agentId) {

        }
    };

    @Test
    public void test() {
        List<TaskStatusChangeWatcher> taskStatusChangeWatchers = new LinkedList<>();
        LivingTaskCacheImpl livingTaskCache = new LivingTaskCacheImpl(null, null,
            null, null, null);
        TaskWatcherForCache taskWatcherForCache = new TaskWatcherForCache(taskStatusMachine,
            taskMapper, livingTaskCache);
        taskStatusChangeWatchers.add(taskWatcherForCache);
        CacheWrapperForWatch cacheWrapperForWatch = new CacheWrapperForWatch(livingTaskCache,taskStatusChangeWatchers);
        Job job = mockJob();
        List<Task> mockedTasks = mockTask(job);
        cacheWrapperForWatch.adopt(
            mockedTasks, TaskStatus.CREATED);
        cacheWrapperForWatch.ofStatus(TaskStatus.CREATED).stream().collect(Collectors.toList()).subList(10, 20).forEach(task -> {
            task.setStatus(TaskStatus.RUNNING);
        });
        Assertions.assertEquals(246,
            cacheWrapperForWatch.ofStatus(TaskStatus.CREATED).size());
    }

    private List<Task> mockTask(Job job) {
        List<Task> of = new LinkedList<>();
        for (Long i = 1L; i < 257; i++) {
            of.add(Task.builder()
                .status(TaskStatus.CREATED)
                .id(i)
                .taskRequest(new CMPRequest("hi"))
                .job(job)
                .build());
        }
        return of;
    }

    private Job mockJob() {
        return Job.builder()
            .id(1l)
            .status(JobStatus.CREATED)
            .jobRuntime(new JobRuntime())
            .swmp(new SWModelPackage())
            .swDataSets(new ArrayList<>(0))
            .build();
    }

}
