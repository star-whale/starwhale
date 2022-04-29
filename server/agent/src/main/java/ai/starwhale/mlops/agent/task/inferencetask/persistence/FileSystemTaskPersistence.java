/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.persistence;

import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTaskStatus;
import ai.starwhale.mlops.agent.utils.FileUtil;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <ul>under the basePath,Eg:/var/starwhale/，there have serial path：</ul>
 * <li>tasks</li>
 * <li>swmp</li>
 */
@Slf4j
@Service
public class FileSystemTaskPersistence implements TaskPersistence {

    private final StorageProperties storageProperties;

    private final StorageAccessService storageAccessService;

    private final FileSystemPath fileSystemPath;

    public FileSystemTaskPersistence(StorageProperties storageProperties,
                                     StorageAccessService storageAccessService,
                                     FileSystemPath fileSystemPath) {
        this.storageProperties = storageProperties;
        this.storageAccessService = storageAccessService;
        this.fileSystemPath = fileSystemPath;
    }

    @Override
    public Optional<List<InferenceTask>> getAllActiveTasks() {
        try {
            Path tasksPath = Path.of(fileSystemPath.activeTaskDir());
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
                                .filter(path -> path.getFileName().toString().endsWith(FileSystemPath.FileName.InferenceTaskInfoFile))
                                .map(path -> {
                                    try {
                                        String json = Files.readString(path);
                                        return JSONUtil.toBean(json, InferenceTask.class);
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
    public Optional<InferenceTask> getActiveTaskById(Long id) {
        try {
            // get the newest task info
            Path taskPath = Path.of(fileSystemPath.oneActiveTaskInfoFile(id));
            String json = Files.readString(taskPath);
            return Optional.of(JSONUtil.toBean(json, InferenceTask.class));
        } catch (Exception e) {
            log.error("get task by id:{} occur error:{}", id, e.getMessage(), e);
            return Optional.empty();
        }

    }

    @Override
    public Optional<ExecuteStatus> status(Long id) {
        try {
            // get the newest task info
            Path statusFilePath = Path.of(fileSystemPath.oneActiveTaskStatusFile(id));
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
        Path statusFilePath = Path.of(fileSystemPath.oneActiveTaskStatusFile(id));
        if (Files.notExists(statusFilePath)) {
            Files.createDirectories(Path.of(fileSystemPath.oneActiveTaskStatusDir(id)));
        }
        Files.writeString(statusFilePath, status.name(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return true;
    }

    @Override
    public boolean save(InferenceTask task) {
        try {
            Path taskDirPath = Path.of(fileSystemPath.oneActiveTaskDir(task.getId()));
            if (Files.notExists(taskDirPath)) {
                Files.createDirectories(taskDirPath);
            }
            // update info to the task file
            Files.writeString(Path.of(fileSystemPath.oneActiveTaskInfoFile(task.getId())), JSONUtil.toJsonStr(task), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            if (task.getStatus() == InferenceTaskStatus.ARCHIVED) {
                move2Archived(task);
            }
            return true;
        } catch (Exception e) {
            log.error("save task status occur error:{}", e.getMessage(), e);
            return false;
        }

    }

    /**
     * move task to the archived state
     *
     * @param task task
     * @return if success
     */
    private void move2Archived(InferenceTask task) throws IOException {
        // move to the archived task file
        try {
            FileUtils.moveDirectoryToDirectory(
                    new File(fileSystemPath.oneActiveTaskDir(task.getId())), new File(fileSystemPath.archivedTaskDir()), true);
        } catch (FileExistsException e) {
            String newPath = fileSystemPath.archivedTaskDir() + "repeat/" + task.getId() + "_" + System.currentTimeMillis();
            log.error("already exist task:{}, move to {}", JSONUtil.toJsonStr(task), newPath);
            FileUtils.moveDirectoryToDirectory(new File(fileSystemPath.oneActiveTaskDir(task.getId())), new File(newPath), true);
        }

    }


    @Override
    public void preloadingSWMP(InferenceTask task) throws IOException {
        SWModelPackage model = task.getSwModelPackage();

        String cachePathStr = fileSystemPath.oneSwmpCacheDir(model.getName(), model.getVersion());

        // check if exist todo check with md5
        if (Files.notExists(Path.of(cachePathStr))) {
            download(cachePathStr, model.getPath());
        }
        File targetDir = new File(fileSystemPath.oneActiveTaskModelDir(task.getId()));
        Files.find(Path.of(cachePathStr), 1, (p, u) -> !p.toString().equals(cachePathStr)).forEach(path -> {
            try {
                File src = path.toFile();
                if (Files.isDirectory(path)) {
                    FileUtil.copyDirectoryToDirectory(src, targetDir);
                } else {
                    FileUtil.copyFileToDirectory(src, targetDir);
                }
            } catch (IOException e) {
                log.error("copy swmp:{} to {} error", path, targetDir.getPath());
            }
        });
    }

    /**
     * lock avoid multi task download single file
     */
    private synchronized void download(String localPath, String remotePath) throws IOException {
        if (Files.notExists(Path.of(localPath))) {
            // pull swmp(tar) and uncompress it to the swmp dir
            Stream<String> paths = storageAccessService.list((remotePath));
            paths.collect(Collectors.toList()).forEach(path -> {
                try (InputStream swmpStream = storageAccessService.get(path)) {
                    TarUtil.extractor(swmpStream, localPath);
                } catch (IOException e) {
                    log.error("download swmp file error", e);
                }
            });
        }
    }

    private final String dataFormat = "%s:%s:%s";

    @Override
    public void generateConfigFile(InferenceTask task) throws IOException {

        Path configDir = Path.of(fileSystemPath.oneActiveTaskConfigDir(task.getId()));
        if (Files.notExists(configDir)) {
            Files.createDirectories(configDir);
        }
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
        Path configPath = Path.of(fileSystemPath.oneActiveTaskInputConfigFile(task.getId()));
        switch (task.getTaskType()) {
            case PPL:
                object.set("kind", "swds");
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
                break;
            case CMP:
                object.set("kind", "jsonl");
                JSONArray cmp = JSONUtil.createArray();
                task.getCmpInputFilePaths().forEach(inputFilePath -> {
                    JSONObject ds = JSONUtil.createObj();
                    ds.set("bucket", storageProperties.getS3Config().getBucket());
                    ds.set("key", JSONUtil.createObj()
                            .set("data", inputFilePath)
                    );
                    cmp.add(ds);
                });

                object.set("swds", cmp);
        }

        Files.writeString(configPath, JSONUtil.toJsonStr(object), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public void uploadResult(InferenceTask task) throws IOException {
        // results is a set of files
        uploadLocalDir2Storage(fileSystemPath.oneActiveTaskResultDir(task.getId()), task.getResultPath().resultDir());
    }

    @Override
    public void uploadLog(InferenceTask task) throws IOException {
        // results is a set of files
        uploadLocalDir2Storage(fileSystemPath.oneActiveTaskLogDir(task.getId()), task.getResultPath().logDir());
    }

    private void uploadLocalDir2Storage(String srcDir, String destDir) throws IOException {
        // results is a set of files
        List<Path> results = Files.find(
                Path.of(srcDir), 1, (path, b) -> !Files.isDirectory(path)
        ).collect(Collectors.toList());

        if (CollectionUtil.isNotEmpty(results)) {
            results.forEach(path -> {
                try {
                    storageAccessService.put(destDir + "/" + path.getFileName(),
                            new BufferedInputStream(new FileInputStream(String.valueOf(path))));
                } catch (IOException e) {
                    log.error("upload result:{} occur error:{}", path.getFileName(), e.getMessage(), e);
                }
            });
        } else {
            log.error("dir:{} has no log to upload", srcDir);
        }
    }

    @Override
    public void uploadContainerLog(InferenceTask task, String logPath) {
        try {
            File logFile = new File(logPath);
            storageAccessService.put(task.getResultPath().logDir() + "/" + logFile.getName(),
                    new BufferedInputStream(new FileInputStream(logPath)));
        } catch (IOException e) {
            log.error("upload container log :{} occur error:{}", logPath, e.getMessage(), e);
        }

    }
}
