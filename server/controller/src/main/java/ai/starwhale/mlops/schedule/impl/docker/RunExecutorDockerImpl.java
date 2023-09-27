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

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.schedule.executor.RunExecutor;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.reporting.ReportedRun;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;


@Slf4j
public class RunExecutorDockerImpl implements RunExecutor {

    public static Map<String, String> CONTAINER_LABELS = Map.of("owner", "starwhale");

    final DockerClientFinder dockerClientFinder;
    final HostResourceConfigBuilder hostResourceConfigBuilder;
    final ExecutorService cmdExecThreadPool;
    final ContainerRunMapper containerRunMapper;
    final String nodeIp;
    final String network;

    public RunExecutorDockerImpl(
            DockerClientFinder dockerClientFinder,
            HostResourceConfigBuilder hostResourceConfigBuilder,
            ExecutorService cmdExecThreadPool,
            ContainerRunMapper containerRunMapper,
            String nodeIp,
            String network
    ) {
        this.dockerClientFinder = dockerClientFinder;
        this.hostResourceConfigBuilder = hostResourceConfigBuilder;
        this.cmdExecThreadPool = cmdExecThreadPool;
        this.containerRunMapper = containerRunMapper;
        this.nodeIp = nodeIp;
        this.network = network;
    }

    @Override
    public void run(Run run, RunReportReceiver runReportReceiver) {

        DockerClient dockerClient = dockerClientFinder.findProperDockerClient(run.getRunSpec().getResourcePool());
        var runSpec = run.getRunSpec();
        String image = runSpec.getImage();
        dockerClient.pullImageCmd(image).exec(new ResultCallback<PullResponseItem>() {
            @Override
            public void onStart(Closeable closeable) {
                ReportedRun reportedRun = ReportedRun.builder()
                        .id(run.getId())
                        .status(RunStatus.PENDING)
                        .ip(nodeIp)
                        .build();
                runReportReceiver.receive(reportedRun);
            }

            @Override
            public void onNext(PullResponseItem object) {

            }

            @Override
            public void onError(Throwable throwable) {
                log.error("creating container error ", throwable);
                ReportedRun reportedRun = ReportedRun.builder()
                        .id(run.getId())
                        .status(RunStatus.FAILED)
                        .startTimeMillis(System.currentTimeMillis())
                        .ip(nodeIp)
                        .stopTimeMillis(System.currentTimeMillis())
                        .failedReason(throwable.getMessage())
                        .build();
                runReportReceiver.receive(reportedRun);

            }

            @Override
            public void onComplete() {
                var labels = new HashMap<String, String>();
                labels.put(ContainerRunMapper.CONTAINER_LABEL_RUN_ID, run.getId().toString());
                labels.putAll(CONTAINER_LABELS);

                CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image)
                        .withEnv(buildEnvs(runSpec.getEnvs()))
                        .withName(containerRunMapper.containerName(run))
                        .withHostConfig(hostResourceConfigBuilder
                                                .build(run.getRunSpec().getRequestedResources())
                                                .withNetworkMode(network))
                        .withLabels(labels);
                ContainerCommand containerCommand = runSpec.getCommand();
                if (null != containerCommand.getEntrypoint()) {
                    createContainerCmd.withEntrypoint(containerCommand.getEntrypoint());
                }
                if (null != containerCommand.getCmd()) {
                    createContainerCmd.withCmd(containerCommand.getCmd());
                }
                CreateContainerResponse createContainerResponse = createContainerCmd.exec();
                dockerClient.startContainerCmd(createContainerResponse.getId()).exec();
                ReportedRun reportedRun = ReportedRun.builder()
                        .id(run.getId())
                        .ip(nodeIp)
                        .status(RunStatus.RUNNING)
                        .startTimeMillis(System.currentTimeMillis())
                        .build();
                runReportReceiver.receive(reportedRun);
            }

            @Override
            public void close() throws IOException {

            }
        });
    }

    @Override
    public void stop(Run run) {
        DockerClient dockerClient = dockerClientFinder.findProperDockerClient(run.getRunSpec().getResourcePool());
        Container container = containerRunMapper.containerOfRun(run);
        if (null == container) {
            return;
        }
        try {
            dockerClient.killContainerCmd(container.getId()).exec();
        } catch (DockerException e) {
            log.warn("try to kill container with error", e);
        }
    }

    @Override
    public void remove(Run run) {
        DockerClient dockerClient = dockerClientFinder.findProperDockerClient(run.getRunSpec().getResourcePool());
        Container container = containerRunMapper.containerOfRun(run);
        if (null == container) {
            return;
        }
        if (!"exited".equalsIgnoreCase(container.getState())) {
            log.warn("trying to remove container that is not exited yet");
        }
        try {
            dockerClient.removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
        } catch (DockerException e) {
            log.warn("try to remove container with error", e);
        }
    }

    @Override
    public Future<String[]> exec(Run run, String... command) {

        DockerClient dockerClient = dockerClientFinder.findProperDockerClient(run.getRunSpec().getResourcePool());
        var execCommand = List.of("sh", "-c", String.join(" ", command)).toArray(new String[0]);

        ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerRunMapper.containerOfRun(run).getId())
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

    @NotNull
    private List<String> buildEnvs(Map<String, String> env) {
        return env.entrySet().stream().map(
                es -> String.format("%s=%s", es.getKey(), es.getValue())
        ).collect(Collectors.toList());
    }
}
