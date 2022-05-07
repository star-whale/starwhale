/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.persistence;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * <ul>under the basePath,Eg:/var/starwhale/，there have serial path：</ul>
 * <li>tasks</li>
 * <li>swmp</li>
 */
@Slf4j
public class FileSystemPath {

    interface FileName {
        String InferenceTaskInfoFile = "taskInfo.json";
        String InferenceTaskInputConfigFile = "input.json";
        String InferenceTaskStatusFile = "current";
    }

    private final String basePath;

    private static final PathNode baseDir = new PathNode("%s", PathNode.Type.variable, (PathNode) null);


    private static final PathNode tasksDir = new PathNode("tasks", PathNode.Type.value);

    // active task
    private static final PathNode activeTaskDir = new PathNode("active", PathNode.Type.value);
    private static final PathNode oneActiveInferenceTaskDir = new PathNode("%s", PathNode.Type.variable);
    private static final PathNode oneActiveInferenceTaskInfoFile = new PathNode(FileName.InferenceTaskInfoFile, PathNode.Type.value);
    private static final PathNode oneActiveInferenceTaskStatusDir = new PathNode("status", PathNode.Type.value);
    private static final PathNode oneActiveInferenceTaskStatusFile = new PathNode(FileName.InferenceTaskStatusFile, PathNode.Type.value);
    private static final PathNode oneActiveInferenceTaskConfigDir = new PathNode("config", PathNode.Type.value);
    private static final PathNode oneActiveInferenceTaskInputConfigFile = new PathNode(FileName.InferenceTaskInputConfigFile, PathNode.Type.value);
    private static final PathNode oneActiveInferenceTaskResultDir = new PathNode("result", PathNode.Type.value);
    private static final PathNode oneActiveInferenceTaskModelDir = new PathNode("swmp", PathNode.Type.value);
    private static final PathNode oneActiveInferenceTaskLogsDir = new PathNode("log", PathNode.Type.value);

    // archived dir
    private static final PathNode archivedTaskDir = new PathNode("archived", PathNode.Type.value);
    private static final PathNode oneArchivedInferenceTaskDir = new PathNode("%s", PathNode.Type.variable);


    // swmp cache dir
    private static final PathNode swmpCacheDir = new PathNode("swmp", PathNode.Type.value);
    private static final PathNode swmpNameDir = new PathNode("%s", PathNode.Type.variable);
    private static final PathNode oneSwmpDir = new PathNode("%s", PathNode.Type.variable);


    static {
        // config map
        baseDir.child(tasksDir
                        .child(activeTaskDir
                                .child(oneActiveInferenceTaskDir
                                        .child(oneActiveInferenceTaskInfoFile)
                                        .child(oneActiveInferenceTaskStatusDir.child(oneActiveInferenceTaskStatusFile))
                                        .child(oneActiveInferenceTaskConfigDir
                                                .child(oneActiveInferenceTaskInputConfigFile)
                                        )
                                        .child(oneActiveInferenceTaskModelDir)
                                        .child(oneActiveInferenceTaskResultDir)
                                        .child(oneActiveInferenceTaskLogsDir)
                                )
                        )
                        .child(archivedTaskDir
                                .child(oneArchivedInferenceTaskDir)
                        )
                )
                .child(swmpCacheDir
                        .child(swmpNameDir.child(oneSwmpDir))
                );
    }

    public FileSystemPath(String basePath) {
        this.basePath = basePath;
    }


    /**
     * one task's base dir path,Eg:/var/starwhale/task/
     */
    public String activeTaskDir() {
        return activeTaskDir.path(basePath);
    }

    /**
     * @param id taskId
     * @return one task's base dir path,Eg:/var/starwhale/task/{taskId}/
     */
    public String oneActiveTaskDir(Long id) {
        return oneActiveInferenceTaskDir.path(basePath, id);
    }

    /**
     * @param id taskId
     * @return taskInfo dir path,Eg:/var/starwhale/task/{taskId}/taskInfo.json(format:json)
     */
    public String oneActiveTaskInfoFile(Long id) {
        return oneActiveInferenceTaskInfoFile.path(basePath, id);
    }

    /**
     * @param id taskId
     * @return task running status dir path,Eg:/var/starwhale/task/{taskId}/status/current(format:txt)
     */
    public String oneActiveTaskStatusFile(Long id) {
        return oneActiveInferenceTaskStatusFile.path(basePath, id);
    }

    public String oneActiveTaskStatusDir(Long id) {
        return oneActiveInferenceTaskStatusDir.path(basePath, id);
    }

    public String oneActiveTaskConfigDir(Long id) {
        return oneActiveInferenceTaskConfigDir.path(basePath, id);
    }

    public String oneActiveTaskModelDir(Long id) {
        return oneActiveInferenceTaskModelDir.path(basePath, id);
    }

    /**
     * swds config file path,Eg:/var/starwhale/tasks/active/{taskId}/config/input.json(format:json)
     */
    public String oneActiveTaskInputConfigFile(Long id) {
        return oneActiveInferenceTaskInputConfigFile.path(basePath, id);
    }

    /**
     * task result dir path,Eg:/var/starwhale/tasks/active/{taskId}/result/
     */
    public String oneActiveTaskResultDir(Long id) {
        return oneActiveInferenceTaskResultDir.path(basePath, id);
    }

    /**
     * task archived dir path,Eg:/var/starwhale/archived/
     */
    public String archivedTaskDir() {
        return archivedTaskDir.path(basePath);
    }

    /**
     * task archived dir path,Eg:/var/starwhale/tasks/archived/{taskId}/
     */
    public String oneArchivedTaskDir(Long id) {
        return oneArchivedInferenceTaskDir.path(basePath, id);
    }

    /**
     * swmp dir path,Eg:/var/starwhale/task/swmp/{name}/{version}(dir)
     */
    public String oneSwmpCacheDir(String name, String version) {
        return oneSwmpDir.path(basePath, name, version);
    }

    /**
     * task runtime log dir path,Eg:/var/starwhale/[active or archived]/tasks/{taskId}/log
     */
    public String oneActiveTaskLogDir(Long id) {
        return oneActiveInferenceTaskLogsDir.path(basePath, id);
    }

    public static class PathNode {
        public PathNode(String value, Type type) {
            this.value = value;
            this.type = type;
        }

        public PathNode(String value, Type type, PathNode parent) {
            this.value = value;
            this.type = type;
            this.parent = parent;
        }

        public PathNode(String value, Type type, List<PathNode> children) {
            this.value = value;
            this.type = type;
            if (CollectionUtil.isNotEmpty(children)) {
                children.forEach(pathNode -> pathNode.parent = this);
            }
        }

        PathNode parent;
        String value;
        Type type;

        public enum Type {
            value, variable
        }

        public PathNode child(PathNode child) {
            child.parent = this;
            return this;
        }

        public String value() {
            return this.value;
        }

        public String path(Object... objs) {
            List<String> nodes = new LinkedList<>();
            PathNode current = this;
            int argsNum = 0;
            while (current != null) {

                if (current.type == Type.variable) argsNum++;
                nodes.add(current.value);
                current = current.parent;
            }
            if (argsNum != objs.length) throw new IllegalArgumentException();
            Collections.reverse(nodes);
            String format = String.join("/", nodes);
            return String.format(format, objs);
        }
    }
}
