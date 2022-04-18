/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.persistence;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.utils.TarUtil;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.configuration.StorageProperties;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.starwhale.mlops.agent.task.persistence.FileSystemPath.*;

/**
 * <ul>under the basePath,Eg:/var/starwhale/，there have serial path：</ul>
 * <li>task</li>
 * <li>swmp</li>
 */
@Slf4j
@Service
public class FileSystemTaskPersistence implements TaskPersistence {

    private final AgentProperties agentProperties;

    private final StorageProperties storageProperties;

    private final StorageAccessService storageAccessService;

    private final FileSystemPath fileSystemPath;

    public FileSystemTaskPersistence(AgentProperties agentProperties,
                                     StorageProperties storageProperties,
                                     StorageAccessService storageAccessService) {
        this.agentProperties = agentProperties;
        this.storageProperties = storageProperties;
        this.storageAccessService = storageAccessService;
        this.fileSystemPath = new FileSystemPath(this.agentProperties.getBasePath());
    }

    @Override
    public Optional<List<EvaluationTask>> getAllActiveTasks() {
        try {
            Path tasksPath = Path.of(basePathOfActiveTasks());
            if (!Files.exists(tasksPath)) {
                Files.createDirectories(tasksPath);
                log.info("init tasks dir, nothing to rebuild, path:{}", tasksPath);
                return Optional.of(List.of());
            } else {
                // rebuild taskQueue
                Stream<Path> taskInfos = Files.find(tasksPath, 3,
                        (path, basicFileAttributes) -> true);
                return Optional.of(
                        taskInfos
                                .filter(path -> path.getFileName().toString().endsWith(oneActiveEvaluationTaskSwdsConfigFile.value()))
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
                                .collect(Collectors.toList())
                );
            }
        } catch (Exception e) {
            log.error("get all active tasks occur error:{}", e.getMessage(), e);
            return Optional.empty();
        }

    }

    @Override
    public Optional<EvaluationTask> getTaskById(Long id) {
        try {
            // get the newest task info
            Path taskPath = Path.of(pathOfInfoFile(id));
            String json = Files.readString(taskPath);
            return Optional.of(JSONUtil.toBean(json, EvaluationTask.class));
        } catch (Exception e) {
            log.error("get task by id:{} occur error:{}", id, e.getMessage(), e);
            return Optional.empty();
        }

    }

    @Override
    public Optional<ExecuteStatus> status(Long id) {
        try {
            // get the newest task info
            Path statusFilePath = Path.of(pathOfStatusFile(id));
            if (Files.exists(statusFilePath)) {
                String status = Files.readString(statusFilePath);
                if (StringUtils.hasText(status)) {
                    return Optional.of(ExecuteStatus.valueOf(status));
                }
            }

            return Optional.of(ExecuteStatus.unknown);
        } catch (Exception e) {
            log.error("get task container status occur error:{}", e.getMessage(), e);
            return Optional.empty();
        }

    }

    @Override
    public boolean updateStatus(Long id, ExecuteStatus status) throws Exception {
        // get the newest task info
        Path statusFilePath = Path.of(pathOfStatusFile(id));
        if (Files.notExists(statusFilePath)) {
            Files.createDirectories(Path.of(pathOfStatusDir(id)));
        }
        Files.writeString(statusFilePath, status.name(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        return true;
    }

    @Override
    public boolean save(EvaluationTask task) {
        try {
            Path taskDirPath = Path.of(basePathOfTask(task.getId()));
            if (Files.notExists(taskDirPath)) {
                Files.createDirectories(taskDirPath);
            }
            // update info to the task file
            Files.writeString(Path.of(pathOfInfoFile(task.getId())), JSONUtil.toJsonStr(task), StandardOpenOption.CREATE);
            return true;
        } catch (Exception e) {
            log.error("save task status occur error:{}", e.getMessage(), e);
            return false;
        }

    }

    @Override
    public void move2Archived(EvaluationTask task) throws IOException {
        // move to the archived task file
        try {
            FileUtils.moveDirectoryToDirectory(new File(basePathOfTask(task.getId())), new File(pathOfArchived()), true);
        } catch (FileExistsException e) {
            String newPath = pathOfArchived() + "repeat/" + task.getId() + "_" + System.currentTimeMillis();
            log.error("already exist task:{}, move to {}", JSONUtil.toJsonStr(task), newPath);
            FileUtils.moveDirectoryToDirectory(new File(basePathOfTask(task.getId())), new File(newPath), true);
        }

    }

    @Override
    public String preloadingSWMP(EvaluationTask task) throws IOException {
        SWModelPackage model = task.getSwModelPackage();

        String cachePathStr = pathOfSWMPDir(model.getName(), model.getVersion());

        // check if exist todo check with md5
        if (Files.notExists(Path.of(cachePathStr))) {
            // pull swmp(tar) and uncompress it to the swmp dir
            Stream<String> paths = storageAccessService.list((task.getSwModelPackage().getPath()));
            paths.collect(Collectors.toList()).forEach(path -> {
                try (InputStream swmpStream = storageAccessService.get(path)) {
                    TarUtil.extractor(swmpStream, cachePathStr);
                } catch (IOException e) {
                    log.error("download swmp file error", e);
                }
            });
        }
        return cachePathStr;
    }

    private final String dataFormat = "%s:%s:%s";

    @Override
    public void generateSWDSConfig(EvaluationTask task) throws IOException {
        Path configDir = Path.of(pathOfSWDSConfigDir(task.getId()));
        if (Files.notExists(configDir)) {
            Files.createDirectories(configDir);
        }
        String configPathStr = pathOfSWDSConfigFile(task.getId());
        Path configPath = Path.of(configPathStr);
        JSONObject object = JSONUtil.createObj();
        object.set("backend", storageProperties.getType());
        object.set("secret", JSONUtil.createObj()
                .set("access_key", storageProperties.getS3Config().getAccessKey())
                .set("secret_key", storageProperties.getS3Config().getSecretKey())
        );
        object.set("service", JSONUtil.createObj()
                .set("endpoint", storageProperties.getS3Config().getEndpoint())
                .set("region", storageProperties.getS3Config().getRegion())
        );
        JSONArray swds = JSONUtil.createArray();

        task.getSwdsBlocks().forEach(swdsBlock -> {
            JSONObject ds = JSONUtil.createObj();
            ds.set("bucket", storageProperties.getS3Config().getBucket());
            ds.set("key", JSONUtil.createObj()
                    .set("data", String.format(dataFormat, swdsBlock.getLocationInput().getFile(), swdsBlock.getLocationInput().getOffset(), swdsBlock.getLocationInput().getOffset() + swdsBlock.getLocationInput().getSize() - 1))
                    .set("label", String.format(dataFormat, swdsBlock.getLocationLabel().getFile(), swdsBlock.getLocationLabel().getOffset(), swdsBlock.getLocationLabel().getOffset() + swdsBlock.getLocationLabel().getSize() - 1))
            );
            swds.add(ds);
        });
        object.set("swds", swds);
        Files.writeString(configPath, JSONUtil.toJsonStr(object), StandardOpenOption.CREATE);
    }

    @Override
    public void uploadResult(EvaluationTask task) throws IOException {
        // results is a set of files
        Stream<Path> paths = Files.find(Path.of(pathOfResult(task.getId())), 1, (a, b) -> true);
        List<Path> results = paths.filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(results)) {
            results.forEach(path -> {
                try {
                    storageAccessService.put(task.getResultPath() + "/" + path.getFileName(),
                            new BufferedInputStream(new FileInputStream(String.valueOf(path))));
                } catch (IOException e) {
                    log.error("upload result:{} occur error:{}", path.getFileName(), e.getMessage(), e);
                }
            });
        } else {
            throw ErrorCode.uploadError.asException(String.format("task:%s has no result to upload", task.getId()));
        }

    }
}
