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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.schedule.TaskCommandGetter;
import ai.starwhale.mlops.schedule.TaskCommandGetter.TaskCommand;
import ai.starwhale.mlops.schedule.TaskRunningEnvBuilder;
import ai.starwhale.mlops.schedule.impl.docker.reporting.DockerTaskReporter;
import ai.starwhale.mlops.schedule.reporting.ReportedTask;
import ai.starwhale.mlops.schedule.reporting.TaskReportReceiver;
import com.github.dockerjava.api.DockerClient;
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

public class SwTaskSchedulerDockerTest {

    DockerClientFinder dockerClientFinder;
    ContainerTaskMapper containerTaskMapper;
    DockerTaskReporter dockerTaskReporter;
    ExecutorService cmdExecThreadPool;
    TaskRunningEnvBuilder taskRunningEnvBuilder;
    String network;
    String nodeIp;
    SwTaskSchedulerDocker swTaskSchedulerDocker;

    TaskCommandGetter taskCommandGetter;

    static final String IMAGE_BUSY_BOX = "busybox:latest";

    DockerClient dockerClient;

    TaskReportReceiver taskReportReceiver;

    @BeforeEach
    public void setup() {
        taskReportReceiver = mock(TaskReportReceiver.class);
        dockerClientFinder = mock(DockerClientFinder.class);
        dockerClient = localDocker();
        when(dockerClientFinder.findProperDockerClient(any())).thenReturn(dockerClient);
        containerTaskMapper = mock(ContainerTaskMapper.class);
        when(containerTaskMapper.containerNameOfTask(any())).thenReturn("sw-ut-busybox");
        dockerTaskReporter = mock(DockerTaskReporter.class);
        cmdExecThreadPool = Executors.newCachedThreadPool();
        ;
        taskRunningEnvBuilder = mock(TaskRunningEnvBuilder.class);
        taskCommandGetter = mock(TaskCommandGetter.class);
        network = "host";
        nodeIp = "127.1.0.2";
        swTaskSchedulerDocker = new SwTaskSchedulerDocker(
                dockerClientFinder,
                containerTaskMapper,
                dockerTaskReporter,
                cmdExecThreadPool,
                taskRunningEnvBuilder,
                network,
                nodeIp,
                taskCommandGetter);
        try {
            dockerClient.removeContainerCmd("sw-ut-busybox").withForce(true).exec();
        } catch (Exception e) {

        }


    }

    @Test
    public void testExec() throws ExecutionException, InterruptedException {
        Task task = Task.builder()
                .id(1L)
                .step(Step.builder()
                        .job(Job.builder().jobRuntime(JobRuntime.builder().image(IMAGE_BUSY_BOX).build()).build())
                        .build()
                )
                .build();
        testSchedule(task);
        Object lock = new Object();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                List<ReportedTask> reportedTasks = (List<ReportedTask>) invocationOnMock.getArguments()[0];
                if (reportedTasks.get(0).getStatus() == TaskStatus.RUNNING) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
                return null;
            }
        }).when(taskReportReceiver).receive(anyList());
        synchronized (lock) {
            lock.wait();
        }
        Future<String[]> future = swTaskSchedulerDocker.exec(task, "echo", "$ENV_NAME");
        String[] strings = future.get();
        Assertions.assertEquals("STDOUT: env_value", strings[0]);
        testStop(task);
    }

    private void testStop(Task task) {
        swTaskSchedulerDocker.stop(Set.of(task));
        var lc = dockerClient.listContainersCmd().withNameFilter(Set.of("sw-ut-busybox")).withShowAll(true).exec();
        Assertions.assertEquals(0, lc.size());

    }

    private void testSchedule(Task task) {
        when(taskCommandGetter.getCmd(any())).thenReturn(
                new TaskCommand(new String[]{"tail", "-f", "/dev/null"}, null));
        when(taskRunningEnvBuilder.buildCoreContainerEnvs(any())).thenReturn(Map.of("ENV_NAME", "env_value"));

        swTaskSchedulerDocker.schedule(Set.of(task), taskReportReceiver);

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
