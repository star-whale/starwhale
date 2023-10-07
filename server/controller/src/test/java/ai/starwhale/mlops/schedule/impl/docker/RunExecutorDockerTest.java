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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunSpec;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.schedule.impl.container.ContainerCommand;
import ai.starwhale.mlops.schedule.reporting.ReportedRun;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class RunExecutorDockerTest {

    static final String IMAGE_BUSY_BOX = "busybox:latest";
    DockerClientFinder dockerClientFinder;
    ContainerRunMapper containerRunMapper;
    ExecutorService cmdExecThreadPool;
    String network;
    String nodeIp;
    RunExecutorDockerImpl runExecutorDocker;
    DockerClient dockerClient;

    RunReportReceiver runReportReceiver;

    Run run;

    @BeforeEach
    public void setup() {
        runReportReceiver = mock(RunReportReceiver.class);
        dockerClientFinder = mock(DockerClientFinder.class);
        dockerClient = localDocker();
        run = Run.builder()
                .id(1L)
                .runSpec(RunSpec.builder()
                                 .envs(Map.of("ENV_NAME", "env_value"))
                                 .image(IMAGE_BUSY_BOX)
                                 .command(new ContainerCommand(new String[]{"tail", "-f", "/dev/null"}, null))
                                 .requestedResources(List.of())
                                 .resourcePool(null)
                                 .build()
                )
                .build();
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(dockerClient);
        containerRunMapper = mock(ContainerRunMapper.class);
        String containerName = "sw-ut-busybox";
        Container container = mock(Container.class);
        when(container.getId()).thenReturn(containerName);
        when(container.getState()).thenReturn("exited");
        when(containerRunMapper.containerOfRun(any())).thenReturn(container);
        when(containerRunMapper.containerName(any())).thenReturn(containerName);
        cmdExecThreadPool = Executors.newCachedThreadPool();
        network = "host";
        nodeIp = "127.1.0.2";
        runExecutorDocker = new RunExecutorDockerImpl(
                dockerClientFinder,
                new HostResourceConfigBuilder(),
                cmdExecThreadPool,
                containerRunMapper,
                nodeIp,
                network
        );
        try {
            dockerClient.removeContainerCmd(containerName).withForce(true).exec();
        } catch (Exception e) {
            System.out.println("sw-ut-busybox may not exist");
        }


    }

    @Test
    public void testExec() throws ExecutionException, InterruptedException {
        testSchedule(run);
        Object lock = new Object();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                ReportedRun reportedRun = (ReportedRun) invocationOnMock.getArguments()[0];
                if (reportedRun.getStatus() == RunStatus.RUNNING
                        || reportedRun.getStatus() == RunStatus.FAILED) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
                return null;
            }
        }).when(runReportReceiver).receive(any());
        synchronized (lock) {
            //a timeout is set in case there are errors in the test where there is no chance a container is started.
            lock.wait(1000 * 60 * 5);
        }
        Future<String[]> future = runExecutorDocker.exec(run, "echo", "$ENV_NAME");
        String[] strings = future.get();
        Assertions.assertEquals("env_value", strings[0].replace("STDOUT:", "").strip());
        testStopAndRemove(run);
    }

    private void testStopAndRemove(Run run) {
        runExecutorDocker.stop(run);
        var lc = dockerClient.listContainersCmd().withNameFilter(Set.of("sw-ut-busybox")).withShowAll(true).exec();
        Assertions.assertEquals(1, lc.size());
        runExecutorDocker.remove(run);
        lc = dockerClient.listContainersCmd().withNameFilter(Set.of("sw-ut-busybox")).withShowAll(true).exec();
        Assertions.assertEquals(0, lc.size());
    }

    private void testSchedule(Run run) {
        runExecutorDocker.run(run, runReportReceiver);

    }

    DockerClient localDocker() {
        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock").build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(clientConfig, httpClient);
    }

}
