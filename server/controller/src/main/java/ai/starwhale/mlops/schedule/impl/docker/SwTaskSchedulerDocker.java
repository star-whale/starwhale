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

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.impl.container.ContainerSpecification;
import ai.starwhale.mlops.schedule.impl.container.TaskContainerSpecificationFinder;
import ai.starwhale.mlops.schedule.impl.docker.reporting.DockerTaskReporter;
import ai.starwhale.mlops.schedule.reporting.ReportedTask;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

@Slf4j
public class SwTaskSchedulerDocker implements SwTaskScheduler {

    public static Map<String, String> CONTAINER_LABELS = Map.of("owner", "starwhale");
    final DockerClientFinder dockerClientFinder;
    final ContainerTaskMapper containerTaskMapper;
    final DockerTaskReporter dockerTaskReporter;
    final ExecutorService cmdExecThreadPool;
    final TaskContainerSpecificationFinder taskContainerSpecificationFinder;
    final String network;
    final String nodeIp;

    final HostResourceConfigBuilder hostResourceConfigBuilder;

    public SwTaskSchedulerDocker(DockerClientFinder dockerClientFinder, ContainerTaskMapper containerTaskMapper,
            DockerTaskReporter dockerTaskReporter, ExecutorService cmdExecThreadPool,
            TaskContainerSpecificationFinder taskContainerSpecificationFinder, String network, String nodeIp,
            HostResourceConfigBuilder hostResourceConfigBuilder) {
        this.dockerClientFinder = dockerClientFinder;
        this.containerTaskMapper = containerTaskMapper;
        this.dockerTaskReporter = dockerTaskReporter;
        this.cmdExecThreadPool = cmdExecThreadPool;
        this.taskContainerSpecificationFinder = taskContainerSpecificationFinder;
        this.network = network;
        this.nodeIp = nodeIp;
        this.hostResourceConfigBuilder = hostResourceConfigBuilder;
    }

    @Override
    public void schedule(Collection<Task> tasks, TaskReportReceiver taskReportReceiver) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        for (Task task : tasks) {
            DockerClient dockerClient = dockerClientFinder.findProperDockerClient(
                    task.getStep().getResourcePool());
            ContainerSpecification containerSpecification = taskContainerSpecificationFinder.findCs(task);
            String image = containerSpecification.getImage();
            dockerClient.pullImageCmd(image).exec(new ResultCallback<PullResponseItem>() {
                @Override
                public void onStart(Closeable closeable) {
                    ReportedTask rt = ReportedTask.builder()
                            .id(task.getId())
                            .status(TaskStatus.PREPARING)
                            .startTimeMillis(System.currentTimeMillis())
                            .retryCount(0)
                            .ip(nodeIp)
                            .build();
                    taskReportReceiver.receive(List.of(rt));
                }

                @Override
                public void onNext(PullResponseItem object) {

                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("creating container error ", throwable);
                    ReportedTask rt = ReportedTask.builder()
                            .id(task.getId())
                            .status(TaskStatus.FAIL)
                            .stopTimeMillis(System.currentTimeMillis())
                            .retryCount(0)
                            .failedReason(throwable.getMessage())
                            .ip(nodeIp)
                            .build();
                    taskReportReceiver.receive(List.of(rt));

                }

                @Override
                public void onComplete() {
                    Map labels = new HashMap();
                    labels.put(ContainerTaskMapper.CONTAINER_LABEL_TASK_ID, task.getId().toString());
                    labels.putAll(CONTAINER_LABELS);

                    CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image)
                            .withEnv(buildEnvs(containerSpecification.getContainerEnvs()))
                            .withName(containerTaskMapper.containerName(task))
                            .withHostConfig(hostResourceConfigBuilder
                                    .build(task.getTaskRequest().getRuntimeResources())
                                    .withNetworkMode(network))
                            .withLabels(labels);
                    ContainerCommand containerCommand = containerSpecification.getCmd();
                    if (null != containerCommand.getEntrypoint()) {
                        createContainerCmd.withEntrypoint(containerCommand.getEntrypoint());
                    }
                    if (null != containerCommand.getCmd()) {
                        createContainerCmd.withCmd(containerCommand.getCmd());
                    }
                    CreateContainerResponse createContainerResponse = createContainerCmd.exec();
                    dockerClient.startContainerCmd(createContainerResponse.getId()).exec();
                    ReportedTask rt = ReportedTask.builder()
                            .id(task.getId())
                            .status(TaskStatus.RUNNING)
                            .startTimeMillis(System.currentTimeMillis())
                            .retryCount(0)
                            .ip(nodeIp)
                            .build();
                    taskReportReceiver.receive(List.of(rt));
                }

                @Override
                public void close() throws IOException {

                }
            });
        }


    }

    @NotNull
    private List<String> buildEnvs(Map<String, String> env) {
        List<String> envs = env.entrySet().stream().map(
                es -> String.format("%s=%s", es.getKey(), es.getValue())
        ).collect(Collectors.toList());
        return envs;
    }

    @Override
    public void stop(Collection<Task> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        tasks.forEach(t -> {
            DockerClient dockerClient = dockerClientFinder.findProperDockerClient(
                    t.getStep().getResourcePool());
            Container container = containerTaskMapper.containerOfTask(t);
            if (null == container) {
                return;
            }
            if ("exited".equalsIgnoreCase(container.getState())) {
                try {
                    dockerClient.removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
                } catch (DockerException e) {
                    log.warn("try to remove container with error", e);
                }
            } else {
                try {
                    dockerClient.killContainerCmd(container.getId()).exec();
                } catch (DockerException e) {
                    log.warn("try to kill container with error", e);
                }
            }

        });

    }

    @Override
    public Future<String[]> exec(Task task, String... command) {
        DockerClient dockerClient = dockerClientFinder.findProperDockerClient(task.getStep().getResourcePool());
        var execCommand = List.of("sh", "-c", String.join(" ", command)).toArray(new String[0]);

        ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerTaskMapper.containerOfTask(task).getId())
                .withCmd(execCommand)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true);
        ExecCreateCmdResponse exec = execCreateCmd.exec();
        ExecStartCmd execStartCmd = dockerClient.execStartCmd(exec.getId());
        Object lock = new Object();
        StringBuilder stringBuilder = new StringBuilder();
        execStartCmd.exec(new ResultCallback<Frame>() {
            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onNext(Frame object) {
                stringBuilder.append(object.toString());
            }

            @Override
            public void onError(Throwable throwable) {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }

            @Override
            public void onComplete() {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }

            @Override
            public void close() throws IOException {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        });

        return cmdExecThreadPool.submit(() -> {
            synchronized (lock) {
                lock.wait();
            }

            return new String[]{stringBuilder.toString(), ""};
        });
    }

}
