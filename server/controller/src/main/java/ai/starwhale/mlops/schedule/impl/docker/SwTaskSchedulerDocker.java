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
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.SwTaskScheduler;
import ai.starwhale.mlops.schedule.TaskCommandGetter;
import ai.starwhale.mlops.schedule.TaskCommandGetter.TaskCommand;
import ai.starwhale.mlops.schedule.TaskRunningEnvBuilder;
import ai.starwhale.mlops.schedule.impl.docker.reporting.DockerTaskReporter;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceOverwriteSpec;
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
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

@Slf4j
public class SwTaskSchedulerDocker implements SwTaskScheduler {

    final DockerClientFinder dockerClientFinder;

    final ContainerTaskMapper containerTaskMapper;

    final DockerTaskReporter dockerTaskReporter;

    final ExecutorService cmdExecThreadPool;

    final TaskRunningEnvBuilder taskRunningEnvBuilder;

    final String network;

    final String nodeIp;

    final TaskCommandGetter taskCommandGetter;

    public SwTaskSchedulerDocker(DockerClientFinder dockerClientFinder, ContainerTaskMapper containerTaskMapper,
            DockerTaskReporter dockerTaskReporter, ExecutorService cmdExecThreadPool,
            TaskRunningEnvBuilder taskRunningEnvBuilder, String network, String nodeIp,
            TaskCommandGetter taskCommandGetter) {
        this.dockerClientFinder = dockerClientFinder;
        this.containerTaskMapper = containerTaskMapper;
        this.dockerTaskReporter = dockerTaskReporter;
        this.cmdExecThreadPool = cmdExecThreadPool;
        this.taskRunningEnvBuilder = taskRunningEnvBuilder;
        this.network = network;
        this.nodeIp = nodeIp;
        this.taskCommandGetter = taskCommandGetter;
    }

    public static Map<String, String> CONTAINER_LABELS = Map.of("owner", "starwhale");

    @Override
    public void schedule(Collection<Task> tasks, TaskReportReceiver taskReportReceiver) {
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

                    CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image)
                            .withEnv(buildEnvs(task))
                            .withName(containerTaskMapper.containerNameOfTask(task))
                            .withHostConfig(buildHostConfig(task))
                            .withLabels(CONTAINER_LABELS);
                    TaskCommand taskCommand = taskCommandGetter.getCmd(task);
                    if(null != taskCommand.getEntrypoint()){
                        createContainerCmd.withEntrypoint(taskCommand.getEntrypoint());
                    }else if(null != taskCommand.getCmd()){
                        createContainerCmd.withCmd(taskCommand.getCmd());
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
            String containerId = containerTaskMapper.containerNameOfTask(t);
            List<Container> containers = dockerClient.listContainersCmd().withNameFilter(Set.of(containerId)).exec();
            if(CollectionUtils.isEmpty(containers)){
                return;
            }
            try{
                dockerClient.killContainerCmd(containerId).exec();
            }catch (DockerException e){
                log.warn("try to kill container with error", e);
            }
            // CANCELLING tasks need to report to stats watchers once more than failed and success tasks
            // so that it could transfer to CANCELLED
            if(t.getStatus() == TaskStatus.CANCELLING){
                cmdExecThreadPool.submit(()->{
                    while (true){
                        var lc = dockerClient.listContainersCmd().withNameFilter(Set.of(containerId)).withShowAll(true).exec();
                        if(CollectionUtils.isEmpty(lc)){
                            break;
                        }
                        if(lc.get(0).getState().equalsIgnoreCase("running")){
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                log.error("waiting for next watch of CANCELLING task interrupted start next loop immediately",e);
                            }
                            continue;
                        }
                        dockerTaskReporter.reportTask(lc.get(0));
                        try{
                            dockerClient.removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true).exec();
                        }catch (DockerException e){
                            log.warn("try to remove container with error", e);
                        }
                        break;
                    }

                });

            }else {
                try{
                    dockerClient.removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true).exec();
                }catch (DockerException e){
                    log.warn("try to remove container with error", e);
                }
            }


        });

    }

    @Override
    public Future<String[]> exec(Task task, String... command) {
        DockerClient dockerClient = dockerClientFinder.findProperDockerClient(task.getStep().getResourcePool());
        var execCommand = List.of("sh", "-c", String.join(" ", command)).toArray(new String[0]);

        ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerTaskMapper.containerNameOfTask(task))
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
                synchronized (lock){
                    lock.notifyAll();
                }
            }

            @Override
            public void onComplete() {
                synchronized (lock){
                    lock.notifyAll();
                }
            }

            @Override
            public void close() throws IOException {
                synchronized (lock){
                    lock.notifyAll();
                }
            }
        });

        return cmdExecThreadPool.submit(()->{
            synchronized (lock){
                lock.wait();
            }

            return new String[]{stringBuilder.toString(),""};
        });
    }

}
