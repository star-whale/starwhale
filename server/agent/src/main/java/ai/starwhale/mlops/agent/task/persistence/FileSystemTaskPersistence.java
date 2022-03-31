/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.persistence;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.storage.StorageAccessService;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class FileSystemTaskPersistence implements TaskPersistence {

    private final String infoSuffix = ".taskinfo";
    private final String statusSuffix = ".status";

    @Autowired
    private AgentProperties agentProperties;
    @Autowired
    private StorageAccessService storageAccessService;

    @Override
    public List<EvaluationTask> getAllActiveTasks() throws Exception {
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
                    .filter(path -> path.getFileName().toString().endsWith(infoSuffix))
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
    public EvaluationTask getTaskById(Long id) throws Exception {
        String path = agentProperties.getTask().getInfoPath();
        // get the newest task info
        Path taskPath = Path.of(path + "/" + id + infoSuffix);
        String json = Files.readString(taskPath);
        return JSONUtil.toBean(json, EvaluationTask.class);
    }

    @Override
    public Task.TaskStatus getTaskStatusById(Long id) throws Exception {
        String path = agentProperties.getTask().getStatusPath();
        // get the newest task info
        Path taskPath = Path.of(path + "/" + id + statusSuffix);
        String status = Files.readString(taskPath);
        return Task.TaskStatus.valueOf(status);
    }

    @Override
    public boolean save(EvaluationTask task) throws IOException {

        String path = agentProperties.getTask().getInfoPath();
        Path taskPath = Path.of(path + "/" + task.getTask().getId() + infoSuffix);
        if (!Files.exists(taskPath)) {
            Files.createFile(taskPath);
        }
        // update info to the task file
        Files.writeString(taskPath, JSONUtil.toJsonStr(task));
        return true;
    }

    @Override
    public boolean move2Archived(EvaluationTask task) {
        try {
            Path sourcePath = Path.of(
                    agentProperties.getTask().getInfoPath() + "/" + task.getTask().getId()
                            + infoSuffix),
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

    @Override
    public String preloadingSWMP(EvaluationTask task) throws Exception {
        // pull swmp(tar) and uncompress it to the swmp dir
        InputStream swmpStream = storageAccessService.get(task.getSwModelPackage().getPath());
        Path targetDir = Path.of(agentProperties.getTask().getSwmpPath() + "/" + task.getTask().getId() + "/" );
        Files.copy(swmpStream, targetDir);
        // todo untar

        return targetDir.toString();
    }
}
