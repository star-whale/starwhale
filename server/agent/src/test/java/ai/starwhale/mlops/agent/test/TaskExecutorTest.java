/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import ai.starwhale.mlops.agent.StarWhaleAgentApplication;
import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.configuration.AgentProperties.Task;
import ai.starwhale.mlops.agent.configuration.DockerConfiguration;
import ai.starwhale.mlops.agent.configuration.TaskConfiguration;
import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.impl.DockerContainerClient;
import ai.starwhale.mlops.agent.report.ReportHttpClient;
import ai.starwhale.mlops.agent.taskexecutor.SourcePool;
import ai.starwhale.mlops.agent.taskexecutor.TaskExecutor;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskAction.Context;
import ai.starwhale.mlops.agent.taskexecutor.initializer.TaskPoolInitializer;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;

@SpringBootTest(
    classes = StarWhaleAgentTestApplication.class)
@ImportAutoConfiguration({DockerConfiguration.class, TaskConfiguration.class})
@TestPropertySource(
    properties = {"sw.task.rebuild.enabled=false", "sw.task.scheduler.enabled=false"},
    locations = "classpath:application-integrationtest.yaml")
public class TaskExecutorTest {
    @Autowired
    private AgentProperties agentProperties;

    @MockBean
    private ReportHttpClient reportHttpClient;

    @MockBean
    private ContainerClient containerClient;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private TaskSource.TaskPool taskPool;

    @Autowired
    private SourcePool sourcePool;

    @Test
    @DisplayName("Should create a file on a file system")
    void givenUnixSystem_whenCreatingFile_thenCreatedInPath() throws IOException {
        FileSystem fileSystem = Jimfs.newFileSystem();
        String fileName = "newFile.txt";
        // important!
        // Path pathToStore = fileSystem.getPath(agentProperties.getTask().getInfoPath());
        Path pathToStore = fileSystem.getPath("/opt/starwhale/tasks/");
        Files.createDirectories(pathToStore);
        Path filePath = pathToStore.resolve(fileName);

        try {
            Files.createFile(filePath);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        assertTrue(Files.exists(pathToStore.resolve(fileName)));
    }

    void init() throws FileNotFoundException {
        URL taskPathUrl = ResourceUtils.getURL("classpath:tasks");
        TaskAction.rebuildTasks.apply(taskPathUrl.getPath().substring(1), Context.builder().taskPool(taskPool).build());
        sourcePool.setToReady();
    }

    @Test
    public void toPreparingTest() throws IOException {
        init();
        Mockito.when(containerClient.startContainer(any(), any())).thenReturn(Optional.of("2222222"));
        // todo how to deal with file write
        //Mockito.when(Files.writeString(Path.of(anyString()), anyString())).then(Answers.valueOf("test"));

        // do preparing test
        taskExecutor.dealPreparingTasks();

        assertEquals(2, taskPool.preparingTasks.size());

        taskExecutor.monitorRunningTasks();

    }
}
