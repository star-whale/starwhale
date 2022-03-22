/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import cn.hutool.json.JSONUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * execute on every startup
 */
@Slf4j
@Component
public class TaskInitializer implements CommandLineRunner {

    private final AgentProperties agentProperties;

    public TaskInitializer(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("come in!");
        Stream<Path> taskInfos = Files.find(Path.of(agentProperties.getTask().getInfoPath()), 1,
            (path, basicFileAttributes) -> true);
        List<TaskContainer> tasks = taskInfos
            .filter(path -> path.getFileName().toString().endsWith(".taskinfo"))
            .map(path -> {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    Stream<String> lines = reader.lines();
                    String json = lines.collect(Collectors.joining());
                    return JSONUtil.toBean(json, TaskContainer.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        tasks.forEach(task -> log.info(JSONUtil.toJsonStr(task)));
    }

    @Builder
    @Data
    static class TaskContainer {

        private Long taskId; // 8byte
        private Long containerId;// 8byte
        private Status status;// 1byte
        private String version;// 10byte

        enum Status {
            running, finished;
        }

    }
}
