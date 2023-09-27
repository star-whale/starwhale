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

package ai.starwhale.mlops.schedule;

import ai.starwhale.mlops.domain.run.RunEntity;
import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunSpec;
import ai.starwhale.mlops.domain.run.mapper.RunMapper;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.domain.task.status.WatchableTask;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.schedule.executor.RunExecutor;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFinder;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
import java.text.MessageFormat;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
public class SwTaskSchedulerImpl implements SwTaskScheduler {

    final RunExecutor runExecutor;
    final RunReportReceiver runReportReceiver;
    final TaskContainerSpecificationFinder taskContainerSpecificationFinder;

    final RunMapper runMapper;

    public SwTaskSchedulerImpl(
            RunExecutor runExecutor,
            @Lazy RunReportReceiver runReportReceiver,
            TaskContainerSpecificationFinder taskContainerSpecificationFinder,
            RunMapper runMapper
    ) {
        this.runExecutor = runExecutor;
        this.runReportReceiver = runReportReceiver;
        this.taskContainerSpecificationFinder = taskContainerSpecificationFinder;
        this.runMapper = runMapper;
    }

    @Override
    @Transactional
    public void schedule(Task task) {
        synchronized (task) {
            if (task.getStatus() == TaskStatus.CANCELED) {
                log.info("task {} is canceled, skip schedule", task.getId());
                return;
            }
            ContainerSpecification containerSpecification = taskContainerSpecificationFinder.findCs(task);
            RunSpec runSpec = RunSpec.builder()
                    .image(containerSpecification.getImage())
                    .envs(containerSpecification.getContainerEnvs())
                    .command(containerSpecification.getCmd())
                    .resourcePool(task.getStep().getResourcePool())
                    .requestedResources(task.getTaskRequest().getRuntimeResources())
                    .build();
            RunEntity runEntity = RunEntity.builder()
                    .taskId(task.getId())
                    .runSpec(runSpec)
                    .logDir(task.getResultRootPath().logDir())
                    .build();
            runMapper.insert(runEntity);
            Run run = Run.builder()
                    .id(runEntity.getId())
                    .taskId(task.getId())
                    .runSpec(runSpec)
                    .logDir(runEntity.getLogDir())
                    .build();
            task.setCurrentRun(run);
            runExecutor.run(run, runReportReceiver);
        }
    }

    @Override
    public void stop(Task task) {
        synchronized (task) {
            try {
                Run run = task.getCurrentRun();
                if (run != null) {
                    runExecutor.stop(run);
                } else {
                    if (task instanceof WatchableTask) {
                        task = ((WatchableTask) task).unwrap();
                    }
                    task.updateStatus(TaskStatus.CANCELED);
                }
            } catch (Throwable e) {
                log.error("try to stop task {} failed", task.getId(), e);
            }
        }

    }

    @Override
    public Future<String[]> exec(Task task, String... command) {
        Run currentRun = task.getCurrentRun();
        if (null == currentRun) {
            throw new SwValidationException(
                    ValidSubject.TASK,
                    MessageFormat.format("task {0} is not running", task.getId())
            );
        }
        return runExecutor.exec(currentRun, command);
    }
}
