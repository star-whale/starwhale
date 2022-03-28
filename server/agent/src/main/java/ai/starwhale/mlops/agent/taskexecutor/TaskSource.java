/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.container.ImageConfig;
import ai.starwhale.mlops.agent.exception.AllocationException;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.node.SourcePool.AllocateRequest;
import ai.starwhale.mlops.api.ReportApi;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.report.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.ReportResponse;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

public interface TaskSource {

    class TaskPool {

        public final Queue<EvaluationTask> preparingTasks = new ArrayDeque<>();
        public final List<EvaluationTask> runningTasks = new Vector<>();
        public final List<EvaluationTask> uploadingTasks = new Vector<>();
        public final List<EvaluationTask> finishedTasks = new Vector<>();
        public final List<EvaluationTask> archivedTasks = new Vector<>();
        public final List<EvaluationTask> canceledTasks = new Vector<>();
        public final List<EvaluationTask> errorTasks = new Vector<>();
        public final List<Long> needToCancel = new Vector<>();

        public void fill(EvaluationTask task) {
            switch (task.getTask().getStatus()) {
                case CREATED:
                case ASSIGNING:
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
        private volatile boolean ready = false;

        public boolean isReady() {
            return ready;
        }

        public void setToReady() {
            ready = true;
        }
    }

    @Slf4j
    class TaskAction {

        @Builder
        @AllArgsConstructor
        @Data
        public static class Context {

            private SourcePool sourcePool;

            private TaskPool taskPool;

            private ContainerClient containerClient;

            private ReportApi reportApi;

            private AgentProperties agentProperties;

            private final Map<String, Object> values = new HashMap<>();

            public void set(String key, Object obj) {
                values.put(key, obj);
            }

            public <T> T get(String key, Class<T> clazz) {
                return clazz.cast(values.get(key));
            }

            public static Context instance(SourcePool sourcePool, TaskPool taskPool,
                ReportApi reportApi,
                ContainerClient containerClient, AgentProperties agentProperties) {
                return new Context(sourcePool, taskPool, containerClient, reportApi,
                    agentProperties);
            }
        }

        public interface DoTransition<Old, New> {

            default boolean condition(Old old, Context context) {
                return true;
            }

            default New processing(Old old, Context context) throws Exception {
                return null;
            }

            default void success(Old old, New n, Context context) {
            }

            /**
             * when occur some exception
             *
             * @param old old param
             */
            default void fail(Old old, Context context, Exception e) {
            }

            default void apply(Old old, Context context) {
                if (condition(old, context)) {
                    try {
                        New o = processing(old, context);
                        success(old, o, context);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        fail(old, context, e);
                    }
                }
            }
        }

        public interface BaseTransition extends DoTransition<EvaluationTask, EvaluationTask> {

            @Override
            default void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                try {
                    String path = context.agentProperties.getTask().getInfoPath();
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
                return context.taskPool.needToCancel.contains(evaluationTask.getTask().getId());
            }

            @Override
            default EvaluationTask processing(EvaluationTask oldTask, Context context) {
                EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
                newTask.getTask().setStatus(TaskStatus.CANCELED);
                return newTask;
            }

            @Override
            default void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                context.taskPool.canceledTasks.add(newTask);
                // cancel success
                context.taskPool.needToCancel.remove(newTask.getTask().getId());
                BaseTransition.super.success(oldTask, newTask, context);
            }
        }

        public static DoTransition<String, List<EvaluationTask>> rebuildTasks = new DoTransition<>() {
            @Override
            public boolean condition(String basePath, Context context) {
                return StringUtils.hasText(basePath) && !context.taskPool.isReady();
            }

            @Override
            public List<EvaluationTask> processing(String basePath, Context context)
                throws Exception {
                log.info("start to rebuild task pool");
                Path tasksPath = Path.of(basePath);
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
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                }
            }

            @Override
            public void success(String basePath, List<EvaluationTask> tasks, Context context) {
                if (CollectionUtil.isNotEmpty(tasks)) {
                    tasks.forEach(context.taskPool::fill);
                    context.taskPool.setToReady();
                    log.info("rebuild task pool success, size:{}", tasks.size());
                }
            }

            @Override
            public void fail(String basePath, Context context, Exception e) {
                log.info("rebuild task pool from:{} error", basePath);
            }
        };

        public static DoTransition<EvaluationTask, EvaluationTask> init2Preparing = new BaseTransition() {
            @Override
            public boolean condition(EvaluationTask obj, Context context) {
                return obj.getTask().getStatus() == TaskStatus.CREATED
                    || obj.getTask().getStatus() == TaskStatus.ASSIGNING;
            }

            @Override
            public EvaluationTask processing(EvaluationTask oldTask, Context context) {
                EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
                newTask.getTask().setStatus(TaskStatus.PREPARING);
                return newTask;
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                // add the new task to the tail
                context.taskPool.preparingTasks.offer(newTask);
                // update info to the task file
                BaseTransition.super.success(oldTask, newTask, context);
            }

            @Override
            public void fail(EvaluationTask evaluationTask, Context context, Exception e) {
                // nothing to do
            }

        };

        public static DoTransition<EvaluationTask, EvaluationTask> preparing2Running = new BaseTransition() {
            @Override
            public EvaluationTask processing(EvaluationTask oldTask, Context context) {
                // allocate device(GPU or todo CPU) for task
                Set<Device> allocated = context.sourcePool.allocate(
                    AllocateRequest.builder().gpuNum(1).build());
                if (CollectionUtil.isNotEmpty(allocated)) {
                    // allocate device to this task,if fail will throw exception, now it is blocked
                    oldTask.setDevices(allocated);
                    // todo fill with task info
                    ContainerClient containerClient = context.containerClient;
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
                        // should release, but we only detect realtime(this mean allocate is only assign to the task without stateful)
                        // so there is no necessary to release
                        throw new RuntimeException();
                    }
                } else {
                    // todo: 由allocate 时抛出，只要没抛出异常，就算成功了， next time again
                    throw new AllocationException("");
                }
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                // rm from current
                context.taskPool.preparingTasks.remove(oldTask);
                // tail it to the running list
                context.taskPool.runningTasks.add(newTask);
                // update info to the task file
                BaseTransition.super.success(oldTask, newTask, context);
            }

            @Override
            public void fail(EvaluationTask i, Context context, Exception e) {
                // nothing to do, again next time
            }
        };

        public static DoTransition<EvaluationTask, EvaluationTask> preparing2Canceled = new BaseCancelTransition() {
            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                context.taskPool.preparingTasks.remove(oldTask);
                BaseCancelTransition.super.success(oldTask, newTask, context);
            }
        };

        public static DoTransition<EvaluationTask, EvaluationTask> running2Uploading = new BaseTransition() {
            @Override
            public EvaluationTask processing(EvaluationTask runningTask, Context context)
                throws IOException {
                /*try {*/
                String path = context.agentProperties.getTask().getInfoPath();
                // get the newest task info
                Path taskPath = Path.of(path + "/" + runningTask.getTask().getId());
                String json = Files.readString(taskPath);
                return JSONUtil.toBean(json, EvaluationTask.class);
                /*} catch (IOException e) {
                    log.error(e.getMessage());
                }
                return null;*/
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                if (newTask.getTask().getStatus() == TaskStatus.UPLOADING) {
                    context.taskPool.uploadingTasks.add(newTask);
                    // if run success, release device to available device pool todo:is there anything else to do?
                    //context.sourcePool.release(newTask.getDevices());
                    // only update memory list,there is no need to update the disk file(already update by taskContainer)
                    context.taskPool.runningTasks.remove(oldTask);
                } else if (newTask.getTask().getStatus() == TaskStatus.EXIT_ERROR) {
                    context.taskPool.errorTasks.add(newTask);
                    // if run success, release device to available device pool todo:is there anything else to do?
                    //context.sourcePool.release(newTask.getDevices());
                    // only update memory list,there is no need to update the disk file(already update by taskContainer)
                    context.taskPool.runningTasks.remove(oldTask);
                } else {
                    // seem like no other status
                    return;
                }
                // update info to the task file
                BaseTransition.super.success(oldTask, newTask, context);
            }

            @Override
            public void fail(EvaluationTask oldTask, Context context, Exception e) {
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
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                context.taskPool.runningTasks.remove(oldTask);
                BaseCancelTransition.super.success(oldTask, newTask, context);
            }
        };


        public static DoTransition<EvaluationTask, EvaluationTask> uploading2Finished = new BaseTransition() {
            @Override
            public EvaluationTask processing(EvaluationTask oldTask, Context context) {
                EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
                // todo: upload result file to the storage
                newTask.getTask().setResultPaths(List.of(""));
                newTask.getTask().setStatus(TaskStatus.FINISHED);
                return newTask;
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                context.taskPool.uploadingTasks.remove(oldTask);
                context.taskPool.finishedTasks.add(newTask);

                // update info to the task file
                BaseTransition.super.success(oldTask, newTask, context);
            }
        };


        public static DoTransition<EvaluationTask, EvaluationTask> uploading2Canceled = new BaseCancelTransition() {
            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                context.taskPool.uploadingTasks.remove(oldTask);
                BaseCancelTransition.super.success(oldTask, newTask, context);
            }
        };

        public static DoTransition<ReportRequest, ReportResponse> report = new DoTransition<ReportRequest, ReportResponse>() {
            @Override
            public boolean condition(ReportRequest reportRequest, Context context) {
                // valid params
                return reportRequest.getNodeInfo() != null || CollectionUtil.isNotEmpty(
                    reportRequest.getTasks());
            }

            @Override
            public ReportResponse processing(ReportRequest reportRequest, Context context)
                throws Exception {
                // all tasks(exclude archived) should be report to the controller
                // finished/canceled tasks should be snapshot(it means must link current finished that, ensure ...), not only reference!!
                List<Task> finishedTasks = context.taskPool.finishedTasks.stream()
                    .map(EvaluationTask::getTask).collect(Collectors.toList());
                List<Task> canceledTasks = context.taskPool.canceledTasks.stream()
                    .map(EvaluationTask::getTask).collect(Collectors.toList());

                List<Task> all = new ArrayList<>();
                // without stop the world
                all.addAll(new ArrayList<>(
                    context.taskPool.preparingTasks.stream().map(EvaluationTask::getTask)
                        .collect(Collectors.toList())));
                all.addAll(new ArrayList<>(
                    context.taskPool.runningTasks.stream().map(EvaluationTask::getTask)
                        .collect(Collectors.toList())));
                all.addAll(new ArrayList<>(
                    context.taskPool.uploadingTasks.stream().map(EvaluationTask::getTask)
                        .collect(Collectors.toList())));
                all.addAll(finishedTasks);
                all.addAll(new ArrayList<>(
                    context.taskPool.errorTasks.stream().map(EvaluationTask::getTask)
                        .collect(Collectors.toList())));
                all.addAll(canceledTasks);
                reportRequest.setTasks(all);

                // todo
                Node node = Node.builder().ipAddr("").name("").memorySizeGB(0l).devices(null)
                    .build();

                context.set("finished", finishedTasks);
                context.set("canceled", finishedTasks);

                ResponseMessage<ReportResponse> response = context.reportApi.report(reportRequest);
                if (Objects.equals(response.getCode(),
                    "success")) { // todo: when coding completed, change protocol:Code to sdk
                    return response.getData();
                } else {
                    return null;
                }
            }

            @Override
            public void success(ReportRequest reportRequest, ReportResponse response,
                Context context) {
                if (response != null) {
                    @SuppressWarnings("unchecked") List<EvaluationTask> finishedTasks = context.get(
                        "finished", List.class);
                    @SuppressWarnings("unchecked") List<EvaluationTask> canceledTasks = context.get(
                        "canceled", List.class);
                    // when success,archived the finished/canceled task,and rm to the archive dir
                    for (EvaluationTask finishedTask : finishedTasks) {
                        TaskAction.finishedOrCanceled2Archived.apply(finishedTask, context);
                    }
                    for (EvaluationTask canceledTask : canceledTasks) {
                        TaskAction.finishedOrCanceled2Archived.apply(canceledTask, context);
                    }
                    // add controller's new tasks to current queue
                    if (CollectionUtil.isNotEmpty(response.getTasksToRun())) {
                        for (TaskTrigger newTask : response.getTasksToRun()) {
                            TaskAction.init2Preparing.apply(EvaluationTask.builder()
                                .task(newTask.getTask())
                                .swdsBlocks(newTask.getSwdsBlocks())
                                .swModelPackage(newTask.getSwModelPackage())
                                .build(), context);
                        }
                    }
                    // add controller's wait to cancel tasks to current list
                    if (CollectionUtil.isNotEmpty(response.getTaskIdsToCancel())) {
                        context.taskPool.needToCancel.addAll(response.getTaskIdsToCancel());
                    }
                }
            }

            @Override
            public void fail(ReportRequest reportRequest, Context context, Exception e) {
                // do nothing
            }
        };

        public static DoTransition<EvaluationTask, EvaluationTask> finishedOrCanceled2Archived = new BaseTransition() {
            @Override
            public EvaluationTask processing(EvaluationTask oldTask, Context context)
                throws Exception {
                EvaluationTask newTask = BeanUtil.toBean(oldTask, EvaluationTask.class);
                newTask.getTask().setStatus(TaskStatus.ARCHIVED);
                return newTask;
            }

            @Override
            public void success(EvaluationTask oldTask, EvaluationTask newTask, Context context) {
                BaseTransition.super.success(oldTask, newTask, context);
            }
        };

        public interface Condition<Input> {

            boolean judge(Input input);
        }

        public interface SelectOneToExecute<Input, Output> {

            default void apply(Input input, Context context, Condition<Input> condition,
                DoTransition<Input, Output> one, DoTransition<Input, Output> another) {
                if (condition.judge(input)) {
                    one.apply(input, context);
                } else {
                    another.apply(input, context);
                }
            }
        }

    }

}
