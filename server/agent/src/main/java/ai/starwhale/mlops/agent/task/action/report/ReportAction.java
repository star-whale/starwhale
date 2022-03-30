/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.report;

import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.task.EvaluationTask;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.action.DoTransition;
import ai.starwhale.mlops.api.ReportApi;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.report.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.ReportResponse;
import ai.starwhale.mlops.domain.node.Node;
import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.domain.task.TaskTrigger;
import cn.hutool.core.collection.CollectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReportAction implements DoTransition<ReportRequest, ReportResponse> {

    @Autowired
    private TaskPool taskPool;

    @Autowired
    protected SourcePool sourcePool;

    @Autowired
    private ReportApi reportApi;

    @Autowired
    DoTransition<String, List<EvaluationTask>> rebuildTasksAction;

    @Autowired
    DoTransition<EvaluationTask, EvaluationTask> init2PreparingAction;

    @Autowired
    DoTransition<EvaluationTask, EvaluationTask> finishedOrCanceled2ArchivedAction;

    @Override
    public ReportResponse processing(ReportRequest reportRequest, Context context)
        throws Exception {
        // all tasks(exclude archived) should be report to the controller
        // finished/canceled tasks should be snapshot(it means must link current finished that, ensure ...), not only reference!!
        List<EvaluationTask> finishedTasks = List.copyOf(taskPool.finishedTasks);
        List<EvaluationTask> canceledTasks = List.copyOf(taskPool.canceledTasks);

        List<Task> all = new ArrayList<>();
        // without stop the world
        all.addAll(new ArrayList<>(
            taskPool.preparingTasks.stream().map(EvaluationTask::getTask)
                .collect(Collectors.toList())));
        all.addAll(new ArrayList<>(
            taskPool.runningTasks.stream().map(EvaluationTask::getTask)
                .collect(Collectors.toList())));
        all.addAll(new ArrayList<>(
            taskPool.uploadingTasks.stream().map(EvaluationTask::getTask)
                .collect(Collectors.toList())));
        all.addAll(taskPool.finishedTasks.stream()
                .map(EvaluationTask::getTask).collect(Collectors.toList()));
        all.addAll(new ArrayList<>(
            taskPool.errorTasks.stream().map(EvaluationTask::getTask)
                .collect(Collectors.toList())));
        all.addAll(taskPool.canceledTasks.stream()
                .map(EvaluationTask::getTask).collect(Collectors.toList()));
        reportRequest.setTasks(all);

        // todo
        Node node = Node.builder().ipAddr("").name("").memorySizeGB(0l).devices(List.copyOf(sourcePool.getDevices()))
            .build();

        reportRequest.setNodeInfo(node);
        context.set("finished", finishedTasks);
        context.set("canceled", canceledTasks);

        ResponseMessage<ReportResponse> response = reportApi.report(reportRequest);
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
                finishedOrCanceled2ArchivedAction.apply(finishedTask, context);
            }
            for (EvaluationTask canceledTask : canceledTasks) {
                finishedOrCanceled2ArchivedAction.apply(canceledTask, context);
            }
            // add controller's new tasks to current queue
            if (CollectionUtil.isNotEmpty(response.getTasksToRun())) {
                for (TaskTrigger newTask : response.getTasksToRun()) {
                    init2PreparingAction.apply(EvaluationTask.builder()
                        .task(newTask.getTask())
                        .imageId(newTask.getImageId())
                        .swdsBlocks(newTask.getSwdsBlocks())
                        .swModelPackage(newTask.getSwModelPackage())
                        .build(), context);
                }
            }
            // add controller's wait to cancel tasks to current list
            if (CollectionUtil.isNotEmpty(response.getTaskIdsToCancel())) {
                taskPool.needToCancel.addAll(response.getTaskIdsToCancel());
            }
        }
    }

    @Override
    public void fail(ReportRequest reportRequest, Context context, Exception e) {
        // do nothing
    }
}
