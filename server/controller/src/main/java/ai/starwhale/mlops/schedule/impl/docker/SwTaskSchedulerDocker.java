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

package ai.starwhale.mlops.schedule.impl.docker;

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.TaskRunningEnvBuilder;
import ai.starwhale.mlops.schedule.impl.docker.reporting.TaskReporter;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceOverwriteSpec;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.KillContainerCmd;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;

public class SwTaskSchedulerDocker implements SwTaskScheduler {

    final DockerClientFinder dockerClientFinder;

    final ContainerTaskMapper containerTaskMapper;

    final TaskReporter taskReporter;

    final ThreadPoolTaskScheduler cmdExecThreadPool;

    final TaskRunningEnvBuilder taskRunningEnvBuilder;

    final String network;

    public SwTaskSchedulerDocker(DockerClientFinder dockerClientFinder, ContainerTaskMapper containerTaskMapper,
            TaskReporter taskReporter, ThreadPoolTaskScheduler cmdExecThreadPool,
            TaskRunningEnvBuilder taskRunningEnvBuilder, String network) {
        this.dockerClientFinder = dockerClientFinder;
        this.containerTaskMapper = containerTaskMapper;
        this.taskReporter = taskReporter;
        this.cmdExecThreadPool = cmdExecThreadPool;
        this.taskRunningEnvBuilder = taskRunningEnvBuilder;
        this.network = network;
    }

    public static Map<String,String> CONTAINER_LABELS = Map.of("owner","starwhale");

    @Override
    public void schedule(Collection<Task> tasks, TaskReportReceiver taskReportReceiver) {
        taskReporter.setTaskReportReceiverIfNotSet(taskReportReceiver);
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        for (Task task : tasks) {
            DockerClient dockerClient = dockerClientFinder.findProperDockerClient(
                    task.getStep().getResourcePool());
            String image = task.getStep().getJob().getJobRuntime().getImage();
            dockerClient.pullImageCmd(image).exec(new ResultCallback<PullResponseItem>() {
                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(PullResponseItem object) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {
                    dockerClient.createContainerCmd(
                                    image
                            )
                            .withEnv(buildEnvs(task))
                            .withHostConfig(buildHostConfig(task))
                            .withLabels(CONTAINER_LABELS)
                            .exec();
                }

                @Override
                public void close() throws IOException {

                }
            });
            //TODO report to taskReportReceiver, taskUUid renamed to task exec identifier
        }


    }

    @NotNull
    private List<String> buildEnvs(Task task) {
        Map<String, String> containerEnvs = taskRunningEnvBuilder.buildCoreContainerEnvs(task);
        List<String> envs = containerEnvs.entrySet().stream().map(
                es -> String.format("%s=%s", es.getKey(), es.getValue())
        ).collect(Collectors.toList());
        return envs;
    }

    private HostConfig buildHostConfig(Task task) {
        HostConfig hostConfig = HostConfig.newHostConfig().withNetworkMode(network);
        List<RuntimeResource> runtimeResources = taskRunningEnvBuilder.deviceResourceRequirements(task);
        runtimeResources.forEach(runtimeResource -> {
            if (ResourceOverwriteSpec.RESOURCE_CPU.equals(runtimeResource.getType())) {
                hostConfig.withCpuPercent(runtimeResource.getRequest().longValue());
                if (null != runtimeResource.getLimit()) {
                    hostConfig.withCpuQuota(runtimeResource.getLimit().longValue());
                }
            }
            if (ResourceOverwriteSpec.RESOURCE_MEMORY.equals(runtimeResource.getType())) {
                hostConfig.withMemoryReservation(runtimeResource.getRequest().longValue());
                if (null != runtimeResource.getLimit()) {
                    hostConfig.withMemory(runtimeResource.getLimit().longValue());
                    hostConfig.withOomKillDisable(true);
                }
            }
        });
        return hostConfig;
    }

    @Override
    public void stop(Collection<Task> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        tasks.forEach(t -> {
            DockerClient dockerClient = dockerClientFinder.findProperDockerClient(
                    t.getStep().getResourcePool());
            KillContainerCmd cmd = dockerClient.killContainerCmd(
                    containerTaskMapper.containerNameOfTask(t));
            cmd.exec();
        });

    }

    @Override
    public Future<String[]> exec(Task task, String... command) {
//        cmdExecThreadPool.submit()
//        dockerClient.execCreateCmd()
        return null;
    }

}
