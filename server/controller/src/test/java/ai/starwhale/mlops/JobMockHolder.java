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

package ai.starwhale.mlops;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.bo.SWDataSet;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class JobMockHolder {

    AtomicLong atomicLong = new AtomicLong(0);
    StoragePathCoordinator storagePathCoordinator = ObjectMockHolder.storagePathCoordinator();

    public Job mockJob(){

        String jobuuid= UUID.randomUUID().toString();
        String jobDir = storagePathCoordinator.generateResultMetricsPath(jobuuid);
        String swdsPath = storagePathCoordinator.generateSwdsPath("projectname1","swds1", "versionswds1");
        List<Step> steps = new LinkedList<>();
        Job job = Job.builder()
            .id(atomicLong.incrementAndGet())
            .uuid(UUID.randomUUID().toString())
            .jobRuntime(JobRuntime.builder().name("runtime1").version("version1").deviceAmount(1).storagePath(jobDir).deviceClass(Clazz.CPU).build())
            .swmp(SWModelPackage.builder().id(1L).name("swmp1").version("versionsmp1").path(storagePathCoordinator.generateSwmpPath("project1","swmp1","versionsmp1")).build())
            .swDataSets(List.of(SWDataSet.builder().id(1L).name("swds1").version("versionswds1").path(
                swdsPath).size(1024L).build()))
            .status(JobStatus.RUNNING)
            .type(JobType.EVALUATION)
            .steps(steps)
            .outputDir(jobDir)
            .build();
        Step currentStep = mockSteps(job, steps);
        job.setCurrentStep(currentStep);

        return job;
    }

    private Step mockSteps(Job job, List<Step> steps) {
        List<Task> tasks = new LinkedList<>();
        Step step1 = Step.builder().tasks(tasks).job(job).id(atomicLong.incrementAndGet()).name("PPL")
            .status(StepStatus.RUNNING).build();

        mockTasks(step1,tasks,TaskStatus.RUNNING);

        tasks = new LinkedList<>();
        Step step2 = Step.builder().tasks(tasks).job(job).id(atomicLong.incrementAndGet()).name("CMP")
            .status(StepStatus.CREATED).build();
        mockTasks(step2,tasks,TaskStatus.CREATED);

        step1.setNextStep(step2);

        steps.add(step1);
        steps.add(step2);
        return step1;

    }


    private void mockTasks(Step step,List<Task> tasks,TaskStatus taskStatus) {
        String taskUUId = UUID.randomUUID().toString();
        Task build = Task.builder()
            .step(step)
            .status(taskStatus)
            .uuid(taskUUId)
            .id(atomicLong.incrementAndGet())
            .resultRootPath(new ResultPath(
                storagePathCoordinator.generateTaskResultPath(step.getJob().getUuid(), taskUUId)))
            .build();

        Task build2 = Task.builder()
            .step(step)
            .status(taskStatus)
            .uuid(taskUUId)
            .id(atomicLong.incrementAndGet())
            .resultRootPath(new ResultPath(
                storagePathCoordinator.generateTaskResultPath(step.getJob().getUuid(), taskUUId)))
            .build();

        tasks.add(build);
        tasks.add(build2);


    }

}
