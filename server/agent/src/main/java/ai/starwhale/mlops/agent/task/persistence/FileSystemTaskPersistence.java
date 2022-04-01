/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.persistence;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class FileSystemTaskPersistence implements TaskPersistence {

    private final AgentProperties agentProperties;

    public FileSystemTaskPersistence(
        AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    @Override
    public List<EvaluationTask> getAllActiveTasks() throws IOException {
        Path tasksPath = Path.of(agentProperties.getTask().getInfoPath());
        if (!Files.exists(tasksPath)) {
            Files.createDirectories(tasksPath);
            log.info("init tasks dir, nothing to rebuild, path:{}", tasksPath);
            return List.of();
        } else {
            // rebuild taskQueue
            Stream<Path> taskInfos = Files.find(tasksPath, 1,
                (path, basicFileAttributes) -> true);
            return taskInfos
                .filter(path -> path.getFileName().toString().endsWith(".taskinfo"))
                .map(path -> {
                    try {
                        String json = Files.readString(path);
                        return JSONUtil.toBean(json, EvaluationTask.class);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
    }

    @Override
    public EvaluationTask getTaskById(Long id) throws IOException {
        String path = agentProperties.getTask().getInfoPath();
        // get the newest task info
        Path taskPath = Path.of(path + "/" + id + ".taskinfo");
        String json = Files.readString(taskPath);
        return JSONUtil.toBean(json, EvaluationTask.class);
    }

    @Override
    public boolean save(EvaluationTask task) {
        try {
            String path = agentProperties.getTask().getInfoPath();
            Path taskPath = Path.of(path + "/" + task.getId() + ".taskinfo");
            if (!Files.exists(taskPath)) {
                Files.createFile(taskPath);
            }
            // update info to the task file
            Files.writeString(taskPath, JSONUtil.toJsonStr(task));
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean move2Archived(EvaluationTask task) {
        try {
            Path sourcePath = Path.of(
                agentProperties.getTask().getInfoPath() + "/" + task.getId()
                    + ".taskinfo"),
                targetDir = Path.of(agentProperties.getTask().getArchivedDirPath() + "/");
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            // move to the task file
            Files.move(sourcePath, targetDir);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
