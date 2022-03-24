/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.ImageConfig;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import ai.starwhale.mlops.domain.task.EvaluationTask;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Vector;
import lombok.extern.slf4j.Slf4j;

public interface TaskSource {

    class TaskPool {

        public static final Queue<EvaluationTask> newTasks = new ArrayDeque<>();
        public static final Queue<EvaluationTask> preparingTasks = new ArrayDeque<>();
        public static final List<EvaluationTask> runningTasks = new Vector<>();
        public static final List<EvaluationTask> uploadingTasks = new Vector<>();
        public static final List<EvaluationTask> finishedTasks = new Vector<>();
        public static final List<EvaluationTask> archivedTasks = new Vector<>();
        public static final List<EvaluationTask> canceledTasks = new Vector<>();
        public static final List<EvaluationTask> errorTasks = new Vector<>();
        public static final List<Long> needToCancel = new Vector<>();

        public static void fill(EvaluationTask task) {
            switch (task.getTask().getStatus()) {
                case CREATED:
                    newTasks.add(task);
                    break;
                case PREPARING:
                    preparingTasks.add(task);
                    break;
                case RUNNING:
                    runningTasks.add(task);
                    break;
                case UPLOADING:
                    uploadingTasks.add(task);
                    break;
                case FINISHED:
                    finishedTasks.add(task);
                    break;
                case ARCHIVED:
                    archivedTasks.add(task);
                    break;
                case EXIT_ERROR:
                    errorTasks.add(task);
                    break;
                case CANCELED:
                    canceledTasks.add(task);
            }
        }

        /**
         * whether init successfully
         */
        private static volatile boolean ready = false;

        public static boolean isReady() {
            return ready;
        }

        public static void setToReady() {
            ready = true;
        }
    }

    @Slf4j
    class TaskAction {

        public static class Context {

            private Context() {
            }

            private Map<String, Object> values = new HashMap<>();

            public void set(String key, Object obj) {
                values.put(key, obj);
            }

            public <T> T get(String key, Class<T> clazz) {
                return clazz.cast(values.get(key));
            }

            public static Context instance() {
                return new Context();
            }
        }

        public interface DoTransition<Old, New> {

            default boolean condition(Old old, Context context) {
                return true;
            }

            default New processing(Old old, Context context) {
                return null;
            }

            default void success(Old old, New n, Context c) {
            }

            /**
             * when occur some exception
             *
             * @param old old param
             */
            default void fail(Old old, Context c) {
            }

            default void apply(Old old, Context c) {
                if (condition(old, c)) {
                    try {
                        New o = processing(old, c);
                        success(old, o, c);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        fail(old, c);
                    }
                }
            }
        }

        public interface BaseTransition extends DoTransition<EvaluationTask, EvaluationTask> {

            @Override
            default void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                try {
                    String path = c.get("taskInfoPath", String.class);
                    Path taskPath = Path.of(path + "/" + newTask.getTask().getId());
                    if (!Files.exists(taskPath)) {
                        Files.createFile(taskPath);
                    }
                    // update info to the task file
                    Files.writeString(taskPath, JSONUtil.toJsonStr(newTask));
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

        public interface BaseCancelTransition extends BaseTransition {

            @Override
            default boolean condition(EvaluationTask evaluationTask, Context context) {
                return TaskPool.needToCancel.contains(evaluationTask.getTask().getId());
            }

            @Override
            default EvaluationTask processing(EvaluationTask oldTask, Context context) {
                EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
                newTask.getTask().setStatus(TaskStatus.CANCELED);
                return newTask;
            }

            @Override
            default void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                TaskPool.canceledTasks.add(newTask);
                // cancel success
                TaskPool.needToCancel.remove(newTask.getTask().getId());
                BaseTransition.super.success(oldTask, newTask, c);
            }
        }

        public static DoTransition<EvaluationTask, EvaluationTask> init2Preparing = new BaseTransition() {
            @Override
            public boolean condition(EvaluationTask obj, Context c) {
                return obj.getTask().getStatus() == TaskStatus.CREATED;
            }

            @Override
            public EvaluationTask processing(EvaluationTask oldTask, Context c) {
                EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
                newTask.getTask().setStatus(TaskStatus.PREPARING);
                newTask.setDevices(SourcePool.allocate(
                    1)); // pre allocate device to this task,if fail will throw exception
                return newTask;
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                TaskPool.newTasks.remove(oldTask);
                // add the new task to the tail
                TaskPool.preparingTasks.offer(newTask);
                // update info to the task file
                BaseTransition.super.success(oldTask, newTask, c);
            }

            @Override
            public void fail(EvaluationTask taskTrigger, Context c) {
                // nothing to do
            }

        };

        public static DoTransition<EvaluationTask, EvaluationTask> init2Canceled = new BaseCancelTransition() {
            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                TaskPool.newTasks.remove(oldTask);
                BaseCancelTransition.super.success(oldTask, newTask, c);
            }
        };

        public static DoTransition<EvaluationTask, EvaluationTask> preparing2Running = new BaseTransition() {
            @Override
            public EvaluationTask processing(EvaluationTask oldTask, Context c) {
                ContainerClient containerClient = c.get("containerClient", ContainerClient.class);
                // todo fill with task info
                Optional<String> containerId = containerClient.startContainer("",
                    ImageConfig.builder().build());
                // whether the container create and start success
                if (containerId.isPresent()) {
                    EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
                    newTask.setContainerId(containerId.get());
                    newTask.getTask().setStatus(TaskStatus.RUNNING);
                    return newTask;
                } else {
                    // todo: retry or take it to the tail of queue
                    throw new RuntimeException();
                }
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                // rm from current
                TaskPool.preparingTasks.remove(oldTask);
                // tail it to the running list
                TaskPool.runningTasks.add(newTask);
                // update info to the task file
                BaseTransition.super.success(oldTask, newTask, c);
            }

            @Override
            public void fail(EvaluationTask i, Context c) {
                // nothing to do
            }
        };

        public static DoTransition<EvaluationTask, EvaluationTask> preparing2Canceled = new BaseCancelTransition() {
            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                TaskPool.preparingTasks.remove(oldTask);
                BaseCancelTransition.super.success(oldTask, newTask, c);
            }
        };

        public static DoTransition<EvaluationTask, EvaluationTask> running2Uploading = new BaseTransition() {
            @Override
            public EvaluationTask processing(EvaluationTask runningTask, Context c) {
                try {
                    String path = c.get("taskInfoPath", String.class);
                    // get the newest task info
                    Path taskPath = Path.of(path + "/" + runningTask.getTask().getId());
                    String json = Files.readString(taskPath);
                    return JSONUtil.toBean(json, EvaluationTask.class);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                return null;
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                // if run success, release device to available device pool todo:is there anything else to do?
                SourcePool.release(newTask.getDevices());
                // only update memory list,there is no need to update the disk file(already update by taskContainer)
                TaskPool.runningTasks.remove(oldTask);
                if (newTask.getTask().getStatus() == TaskStatus.UPLOADING) {
                    TaskPool.uploadingTasks.add(newTask);

                } else if (newTask.getTask().getStatus() == TaskStatus.EXIT_ERROR) {
                    TaskPool.errorTasks.add(newTask);
                } else {
                    // seem like no other status
                }
                // update info to the task file
                BaseTransition.super.success(oldTask, newTask, c);
            }

            @Override
            public void fail(EvaluationTask oldTask, Context c) {
                // nothing
            }
        };

        public static DoTransition<EvaluationTask, EvaluationTask> running2Canceled = new BaseCancelTransition() {
            @Override
            public EvaluationTask processing(EvaluationTask oldTask, Context context) {
                // todo: stop the container
                return BaseCancelTransition.super.processing(oldTask, context);
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                TaskPool.runningTasks.remove(oldTask);
                BaseCancelTransition.super.success(oldTask, newTask, c);
            }
        };


        public static DoTransition<EvaluationTask, EvaluationTask> uploading2Finished = new BaseTransition() {
            @Override
            public EvaluationTask processing(EvaluationTask oldTask, Context c) {
                EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
                // todo: upload result file to the storage
                newTask.getTask().setResultPaths(List.of(""));
                newTask.getTask().setStatus(TaskStatus.FINISHED);
                return newTask;
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                TaskPool.uploadingTasks.remove(oldTask);
                TaskPool.finishedTasks.add(newTask);

                // update info to the task file
                BaseTransition.super.success(oldTask, newTask, c);
            }
        };


        public static DoTransition<EvaluationTask, EvaluationTask> uploading2Canceled = new BaseCancelTransition() {
            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context c) {
                TaskPool.uploadingTasks.remove(oldTask);
                BaseCancelTransition.super.success(oldTask, newTask, c);
            }
        };

        public interface Condition<Input> {

            boolean apply(Input input);
        }

        public interface SelectOneToExecute<Input, Output> {

            default void apply(Input input, Context context, Condition<Input> condition,
                DoTransition<Input, Output> one, DoTransition<Input, Output> another) {
                if (condition.apply(input)) {
                    one.apply(input, context);
                }
                another.apply(input, context);
            }
        }

    }

}
