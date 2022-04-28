/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task.inferencetask.action.report;

import ai.starwhale.mlops.agent.configuration.AgentProperties;
import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.node.base.SystemDetect;
import ai.starwhale.mlops.agent.node.base.SystemInfo;
import ai.starwhale.mlops.agent.task.Action;
import ai.starwhale.mlops.agent.task.Context;
import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ai.starwhale.mlops.agent.task.inferencetask.LogRecorder;
import ai.starwhale.mlops.agent.task.inferencetask.TaskPool;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportAction implements Action<ReportRequest, ReportResponse> {

    @Autowired
    private TaskPool taskPool;

    @Autowired
    protected SourcePool sourcePool;

    @Autowired
    protected LogRecorder logRecorder;

    @Autowired
    private ReportApi reportApi;

    @Autowired
    private SystemDetect systemDetect;

    @Autowired
    private AgentProperties agentProperties;

    @Autowired
    Action<InferenceTask, InferenceTask> init2PreparingAction;

    @Autowired
    Action<InferenceTask, InferenceTask> archivedAction;

    @Override
    public ReportResponse processing(ReportRequest reportRequest, Context context)
            throws Exception {
        // all tasks(exclude archived) should be report to the controller
        // finished/canceled tasks should be snapshot(it means must link current finished that, ensure ...), not only reference!!
        List<InferenceTask> finishedTasks = List.copyOf(taskPool.succeedTasks);
        List<InferenceTask> canceledTasks = List.copyOf(taskPool.canceledTasks);
        List<InferenceTask> errorTasks = List.copyOf(taskPool.failedTasks);

        List<TaskReport> all = new ArrayList<>();
        // without stop the world
        all.addAll(new ArrayList<>(
                taskPool.preparingTasks.stream().map(task -> task.toTaskReport(logRecorder.generateLogs(task.getId())))
                        .collect(Collectors.toList())));
        all.addAll(new ArrayList<>(
                taskPool.runningTasks.stream().map(task -> task.toTaskReport(logRecorder.generateLogs(task.getId())))
                        .collect(Collectors.toList())));
        all.addAll(new ArrayList<>(
                taskPool.uploadingTasks.stream().map(task -> task.toTaskReport(logRecorder.generateLogs(task.getId())))
                        .collect(Collectors.toList())));
        all.addAll(finishedTasks.stream().map(task -> task.toTaskReport(logRecorder.generateLogs(task.getId())))
                .collect(Collectors.toList()));
        all.addAll(new ArrayList<>(
                taskPool.failedTasks.stream().map(task -> task.toTaskReport(logRecorder.generateLogs(task.getId())))
                        .collect(Collectors.toList())));
        all.addAll(canceledTasks.stream().map(task -> task.toTaskReport(logRecorder.generateLogs(task.getId())))
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
                .ipAddr(agentProperties.getHostIP().equals("127.0.0.1") ? systemInfo.getHostAddress() : agentProperties.getHostIP())
                .agentVersion(agentProperties.getVersion())
                .memorySizeGB(BigInteger.valueOf(systemInfo.getTotalMemory()).divide(FileUtils.ONE_GB_BI).intValue())
                .devices(List.copyOf(sourcePool.getDevices()))
                .build();

        reportRequest.setNodeInfo(node);
        context.set("waitArchivedTasks", new ArrayList<>() {
            {
                addAll(finishedTasks);
                addAll(canceledTasks);
                addAll(errorTasks);
            }
        });

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
            @SuppressWarnings("unchecked") List<InferenceTask> waitArchivedTasks = context.get(
                    "waitArchivedTasks", List.class);
            // when success,archived the finished/canceled/error task,and rm to the archive dir
            for (InferenceTask finishedTask : waitArchivedTasks) {
                archivedAction.apply(finishedTask, null);
            }

            // add controller's new tasks to current queue
            if (CollectionUtil.isNotEmpty(response.getTasksToRun())) {
                for (TaskTrigger newTask : response.getTasksToRun()) {
                    init2PreparingAction.apply(InferenceTask.fromTaskTrigger(newTask), context);
                }
            }
            // add controller's wait to cancel tasks to current list
            if (CollectionUtil.isNotEmpty(response.getTaskIdsToCancel())) {
                taskPool.needToCancel.addAll(response.getTaskIdsToCancel());
            }

            if (CollectionUtil.isNotEmpty(response.getLogReaders())) {
                logRecorder.addRecords(response.getLogReaders());
            }
        }
    }
}
