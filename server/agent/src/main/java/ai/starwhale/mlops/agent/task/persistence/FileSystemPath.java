/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.persistence;

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

    private final String basePath;

    public static PathNode baseDir = new PathNode("%s", PathNode.Type.variable, (PathNode) null);


    public static PathNode tasksDir = new PathNode("tasks", PathNode.Type.value);

    // active evaluation task
    public static PathNode activeTaskDir = new PathNode("active", PathNode.Type.value);
    public static PathNode activeEvaluationTaskDir = new PathNode("evaluation", PathNode.Type.value);
    public static PathNode oneActiveEvaluationTaskDir = new PathNode("%s", PathNode.Type.variable);
    public static PathNode oneActiveEvaluationTaskInfoFile = new PathNode("taskInfo.json", PathNode.Type.value);
    public static PathNode oneActiveEvaluationTaskStatusDir = new PathNode("status", PathNode.Type.value);
    public static PathNode oneActiveEvaluationTaskStatusFile = new PathNode("current", PathNode.Type.value);
    public static PathNode oneActiveEvaluationTaskSwdsConfigDir = new PathNode("config", PathNode.Type.value);
    public static PathNode oneActiveEvaluationTaskSwdsConfigFile = new PathNode("swds.json", PathNode.Type.value);
    public static PathNode oneActiveEvaluationTaskResultDir = new PathNode("result", PathNode.Type.value);
    public static PathNode oneActiveEvaluationTaskLogsDir = new PathNode("log", PathNode.Type.value);

    // active compare task
    public static PathNode activeCompareTaskDir = new PathNode("compare", PathNode.Type.value);
    public static PathNode oneActiveCompareTaskDir = new PathNode("%s", PathNode.Type.variable);

    // archived dir
    public static PathNode archivedTaskDir = new PathNode("archived", PathNode.Type.value);
    public static PathNode archivedEvaluationTaskDir = new PathNode("evaluation", PathNode.Type.value);
    public static PathNode archivedCompareTaskDir = new PathNode("compare", PathNode.Type.value);


    // swmp cache dir
    public static PathNode swmpCacheDir = new PathNode("swmp", PathNode.Type.value);
    public static PathNode swmpNameDir = new PathNode("%s", PathNode.Type.variable);
    public static PathNode swmpVersionDir = new PathNode("%s", PathNode.Type.variable);


    static {
        // config map
        baseDir.child(tasksDir
                        .child(activeTaskDir
                                .child(activeEvaluationTaskDir
                                        .child(oneActiveEvaluationTaskDir
                                                .child(oneActiveEvaluationTaskInfoFile)
                                                .child(oneActiveEvaluationTaskStatusDir.child(oneActiveEvaluationTaskStatusFile))
                                                .child(oneActiveEvaluationTaskSwdsConfigDir.child(oneActiveEvaluationTaskSwdsConfigFile))
                                                .child(oneActiveEvaluationTaskResultDir)
                                                .child(oneActiveEvaluationTaskLogsDir)
                                        )
                                )
                                .child(activeCompareTaskDir.child(oneActiveCompareTaskDir))
                        )
                        .child(archivedTaskDir
                                .child(archivedEvaluationTaskDir)
                                .child(archivedCompareTaskDir)
                        )
                )
                .child(swmpCacheDir
                        .child(swmpNameDir.child(swmpVersionDir))
                );
    }

    class TaskPath {

    }

    public FileSystemPath(String basePath) {
        this.basePath = basePath;
    }

    /**
     * @param id taskId
     * one task's base dir path,Eg:/var/starwhale/task/{taskId}/
     */
    public String basePathOfTask(Long id) {
        return oneActiveEvaluationTaskDir.path(basePath, id);
    }

    /**
     * one task's base dir path,Eg:/var/starwhale/task/
     */
    public String basePathOfActiveTasks() {
        return activeTaskDir.path(basePath);
    }

    /**
     * @param id taskId
     * taskInfo dir path,Eg:/var/starwhale/task/{taskId}/taskInfo.json(format:json)
     */
    public String pathOfInfoFile(Long id) {
        return oneActiveEvaluationTaskInfoFile.path(basePath, id);
    }

    /**
     * @param id taskId
     * task running status dir path,Eg:/var/starwhale/task/{taskId}/status/current(format:txt)
     */
    public String pathOfStatusFile(Long id) {
        return oneActiveEvaluationTaskStatusFile.path(basePath, id);
    }

    private String pathOfStatusDir(Long id) {
        return oneActiveEvaluationTaskStatusDir.path(basePath, id);
    }

    /**
     * @param name model name
     * @param version model version
     * swmp dir path,Eg:/var/starwhale/task/{taskId}/swmp/(dir)
     */
    public String pathOfSWMPDir(String name, String version) {
        return swmpVersionDir.path(basePath, name, version);
    }

    /**
     * @param id taskId
     * swds config file path,Eg:/var/starwhale/task/{taskId}/config/swds.json(format:json)
     */
    public String pathOfSWDSConfigFile(Long id) {
        return oneActiveEvaluationTaskSwdsConfigFile.path(basePath, id);
    }

    private String pathOfSWDSConfigDir(Long id) {
        return oneActiveEvaluationTaskSwdsConfigDir.path(basePath, id);
    }

    /**
     * @param id taskId
     * task result dir path,Eg:/var/starwhale/task/{taskId}/result/
     */
    public String pathOfResult(Long id) {
        return oneActiveEvaluationTaskResultDir.path(basePath, id);
    }

    /**
     * task archived dir path,Eg:/var/starwhale/archived/
     */
    public String pathOfArchived() {
        return archivedTaskDir.path(basePath);
    }

    /**
     * @param id taskId
     * task archived dir path,Eg:/var/starwhale/archived/{taskId}/
     */
    public String pathOfArchived(Long id) {
        return archivedEvaluationTaskDir.path(basePath, id);
    }

    /**
     * task runtime log dir path,Eg:/var/starwhale/task/log/{taskId}/log
     */
    public String pathOfLog(Long id) {
        return oneActiveEvaluationTaskLogsDir.path(basePath, id);
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
