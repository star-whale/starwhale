/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor.initializer;

import ai.starwhale.mlops.agent.taskexecutor.AgentProperties;
import ai.starwhale.mlops.agent.taskexecutor.TaskSource.TaskPool;
import ai.starwhale.mlops.domain.task.EvaluationTask;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * execute on every startup
 */
@Slf4j
@Component
public class TaskPoolInitializer implements CommandLineRunner {

    private final AgentProperties agentProperties;
    private final TaskPool taskPool;

    public TaskPoolInitializer(AgentProperties agentProperties, TaskPool taskPool) {
        this.agentProperties = agentProperties;
        this.taskPool = taskPool;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("start to rebuild task pool");
        // rebuild taskQueue
        Stream<Path> taskInfos = Files.find(Path.of(agentProperties.getTask().getInfoPath()), 1,
            (path, basicFileAttributes) -> true);
        List<EvaluationTask> tasks = taskInfos
            .filter(path -> path.getFileName().toString().endsWith(".taskinfo"))
            .map(path -> {
                try {
                    String json = Files.readString(path);
                    return JSONUtil.toBean(json, EvaluationTask.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        tasks.forEach(taskPool::fill);
        taskPool.setToReady();
        log.info("end of rebuild task pool, size:{}", tasks.size());
    }
}
