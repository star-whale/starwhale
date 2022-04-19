/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.action.report;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.node.base.SystemDetect;
import ai.starwhale.mlops.agent.node.base.SystemInfo;
import ai.starwhale.mlops.agent.task.PPLTask;
import ai.starwhale.mlops.agent.task.TaskPool;
import ai.starwhale.mlops.agent.task.action.Context;
import ai.starwhale.mlops.agent.task.action.DoTransition;
import ai.starwhale.mlops.api.ReportApi;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.node.Node;
import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportAction implements DoTransition<ReportRequest, ReportResponse> {

    @Autowired
    private TaskPool taskPool;

    @Autowired
    protected SourcePool sourcePool;

    @Autowired
    private ReportApi reportApi;

    @Autowired
    private SystemDetect systemDetect;

    @Autowired
    private AgentProperties agentProperties;

    @Autowired
    DoTransition<PPLTask, PPLTask> init2PreparingAction;

    @Autowired
    DoTransition<PPLTask, PPLTask> finishedOrCanceled2ArchivedAction;

    @Override
    public ReportResponse processing(ReportRequest reportRequest, Context context)
            throws Exception {
        // all tasks(exclude archived) should be report to the controller
        // finished/canceled tasks should be snapshot(it means must link current finished that, ensure ...), not only reference!!
        List<PPLTask> finishedTasks = List.copyOf(taskPool.finishedTasks);
        List<PPLTask> canceledTasks = List.copyOf(taskPool.canceledTasks);

        List<TaskReport> all = new ArrayList<>();
        // without stop the world
        all.addAll(new ArrayList<>(
                taskPool.preparingTasks.stream().map(PPLTask::toTaskReport)
                        .collect(Collectors.toList())));
        all.addAll(new ArrayList<>(
                taskPool.runningTasks.stream().map(PPLTask::toTaskReport)
                        .collect(Collectors.toList())));
        all.addAll(new ArrayList<>(
                taskPool.uploadingTasks.stream().map(PPLTask::toTaskReport)
                        .collect(Collectors.toList())));
        all.addAll(finishedTasks.stream().map(PPLTask::toTaskReport)
                .collect(Collectors.toList()));
        all.addAll(new ArrayList<>(
                taskPool.errorTasks.stream().map(PPLTask::toTaskReport)
                        .collect(Collectors.toList())));
        all.addAll(canceledTasks.stream().map(PPLTask::toTaskReport)
                .collect(Collectors.toList()));
        reportRequest.setTasks(all);

        SystemInfo systemInfo = systemDetect.detect()
                .orElse(
                        SystemInfo.builder()
                                .hostAddress("localhost")
                                .availableMemory(0)
                                .totalMemory(0)
                                .build()
                );

        // just deal report device's status if there have some tasks which status are preparing
        /*List devices = (List) SerializationUtils.clone(sourcePool.getDevices());
        if(taskPool.preparingTasks.size() > 0) {

        }*/

        Node node = Node.builder()
                .ipAddr(systemInfo.getHostAddress())
                .agentVersion(agentProperties.getVersion())
                .memorySizeGB(BigInteger.valueOf(systemInfo.getTotalMemory()).divide(FileUtils.ONE_GB_BI).intValue())
                .devices(List.copyOf(sourcePool.getDevices()))
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
    public void fail(ReportRequest reportRequest, Context context, Exception e) {
        log.error("report error:{}", e.getMessage(), e);
    }

    @Override
    public void success(ReportRequest reportRequest, ReportResponse response,
                        Context context) {
        if (response != null) {
            @SuppressWarnings("unchecked") List<PPLTask> finishedTasks = context.get(
                    "finished", List.class);
            @SuppressWarnings("unchecked") List<PPLTask> canceledTasks = context.get(
                    "canceled", List.class);
            // when success,archived the finished/canceled task,and rm to the archive dir
            for (PPLTask finishedTask : finishedTasks) {
                finishedOrCanceled2ArchivedAction.apply(finishedTask, null);
            }
            for (PPLTask canceledTask : canceledTasks) {
                finishedOrCanceled2ArchivedAction.apply(canceledTask, null);
            }
            // add controller's new tasks to current queue
            if (CollectionUtil.isNotEmpty(response.getTasksToRun())) {
                for (TaskTrigger newTask : response.getTasksToRun()) {
                    init2PreparingAction.apply(PPLTask.fromTaskTrigger(newTask), context);
                }
            }
            // add controller's wait to cancel tasks to current list
            if (CollectionUtil.isNotEmpty(response.getTaskIdsToCancel())) {
                taskPool.needToCancel.addAll(response.getTaskIdsToCancel());
            }
        }
    }
}
