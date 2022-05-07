/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.test.agent;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.TaskStatusInterface;
import ai.starwhale.mlops.api.protocol.report.req.ReportRequest;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.api.protocol.report.resp.ReportResponse;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.node.Device.Status;
import ai.starwhale.mlops.domain.node.Node;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * mock agent for Controller
 */
@Slf4j
public class AgentMocker {

    String serverAddress = "http://localhost:8082/api/v1";
    List<DeviceHolder> deviceHolders = List.of(new DeviceHolder(Device.builder().clazz(Clazz.GPU).build()),
        new DeviceHolder(Device.builder().clazz(Clazz.GPU).build()),
        new DeviceHolder(Device.builder().clazz(Clazz.GPU).build()),
        new DeviceHolder(Device.builder().clazz(Clazz.GPU).build()),
        new DeviceHolder(Device.builder().clazz(Clazz.CPU).build()),
        new DeviceHolder(Device.builder().clazz(Clazz.CPU).build()),
        new DeviceHolder(Device.builder().clazz(Clazz.CPU).build()),
        new DeviceHolder(Device.builder().clazz(Clazz.CPU).build()));

    RestTemplate restTemplate = new RestTemplate();

    Map<Long,RunningTask> allTasks = new ConcurrentHashMap<>();

    String ip = "127.0.0.1";

    final String serialNumber;

    public AgentMocker(String ip){
        this.ip = ip;
        this.serialNumber = ip;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DeviceHolder{
        Long taskId;
        Device device;
        ConcurrentLinkedQueue<RunningTask> waitingQueue = new ConcurrentLinkedQueue();
        DeviceHolder(Device d){
            this.device = d;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class RunningTask{
        Long startTime;
        TaskTrigger tt;
        boolean cancel;
        boolean deviceOccupied;
        TaskStatusInterface status;

        public RunningTask(TaskTrigger taskTrigger) {
            startTime = System.currentTimeMillis();
            tt = taskTrigger;
            cancel = false;
            deviceOccupied = false;
            status = TaskStatusInterface.PREPARING;
        }

        public void deviceOccupied() {
            startTime = System.currentTimeMillis();
            deviceOccupied = true;
        }

        //PREPARING, RUNNING, SUCCESS, CANCELING, CANCELED, FAIL;
        TaskStatusInterface changeStatus(){
            if(!cancel && !deviceOccupied){
                return TaskStatusInterface.PREPARING;
            }
            Random r = new Random();
            if(r.nextInt(10000) > 99998){
                log.warn("failure of task {}",tt.getId());
                return TaskStatusInterface.FAIL;
            }
            long runningTime = System.currentTimeMillis() - startTime;
            if(!cancel){
                if(runningTime > 100){
                    return TaskStatusInterface.SUCCESS;
                }
                if(runningTime > 20){
                    return TaskStatusInterface.RUNNING;
                }
                return TaskStatusInterface.PREPARING;
            } else {
                if(runningTime > 2000){
                    return TaskStatusInterface.CANCELED;
                }
                return TaskStatusInterface.CANCELING;
            }
        }

        void statusChange(){
            status = changeStatus();
        }
    }

    static final String reportPath="/report";

    public void start() throws InterruptedException {

        while (true){
            statusChange();
            report();
        }
    }

    private void statusChange() throws InterruptedException {
        Thread.sleep(1000);
        allTasks.values().forEach(RunningTask::statusChange);
        freeDeviceFromFinishedTask();
    }

    private void freeDeviceFromFinishedTask() {
        List<Long> finishedIds = finishedTaskIds();
        this.deviceHolders.stream().filter(deviceHolder -> finishedIds.contains(deviceHolder.getTaskId()))
            .forEach(deviceHolder -> {
                RunningTask runningTask = deviceHolder.getWaitingQueue().poll();
                if(null == runningTask){
                    deviceHolder.setTaskId(null);
                    deviceHolder.getDevice().setStatus(Status.idle);
                }else {
                    deviceHolder.setTaskId(runningTask.getTt().getId());
                    runningTask.deviceOccupied();
                }


            });
    }

    private void report() {

        String reportService = serverAddress + reportPath;
        ReportRequest reportRequest =  ReportRequest.builder().nodeInfo(nodeInfo()).tasks(taskInfo()).build();
        HttpEntity<ReportRequest> request = new HttpEntity<>(reportRequest);
        ResponseEntity<ResponseMessage<ReportResponse>> reportResponseResponseEntity = restTemplate.exchange(reportService, HttpMethod.POST,request, new ParameterizedTypeReference<ResponseMessage<ReportResponse>>(){});
        ResponseMessage<ReportResponse> body = reportResponseResponseEntity.getBody();
        if(!"success".equals(body.getCode())){
            Assertions.fail();
        }
        removeEndedTasks();
        ReportResponse reportResponse = body.getData();
        run(reportResponse.getTasksToRun());
        cancel(reportResponse.getTaskIdsToCancel());
    }

    private void removeEndedTasks() {
        List<Long> finishedIds = finishedTaskIds();
        finishedIds.stream().forEach(id->{
            allTasks.remove(id);
        });

    }

    private List<Long> finishedTaskIds() {
        List<Long> finishedIds = this.allTasks.values().stream().filter(
                runningTask -> runningTask.getStatus() == TaskStatusInterface.SUCCESS
                    || runningTask.getStatus() == TaskStatusInterface.CANCELED
                    || runningTask.getStatus() == TaskStatusInterface.FAIL)
            .map(runningTask -> runningTask.getTt().getId())
            .collect(Collectors.toList());
        return finishedIds;
    }

    private List<TaskReport> taskInfo() {
        return allTasks.values().stream().map(
            runningTask -> TaskReport.builder().taskType(runningTask.getTt().getTaskType())
                .id(runningTask.getTt().getId()).status(runningTask.getStatus()).build()).collect(
            Collectors.toList());
    }

    private Node nodeInfo() {
        return Node.builder().devices(
            deviceInfo())
            .agentVersion("1")
            .ipAddr(ip)
            .serialNumber(this.serialNumber)
            .build();
    }

    private List<Device> deviceInfo() {
        return this.deviceHolders.stream().map(deviceHolder -> {
            Device device = deviceHolder.getDevice();
            if(null != deviceHolder.getTaskId()){
                device.setStatus(Status.busy);
            }else {
                device.setStatus(Status.idle);
            }
            return device;
        }).collect(
            Collectors.toList());
    }

    private void run(List<TaskTrigger> tasksToRun) {
        tasksToRun.stream().forEach(taskTrigger -> {
            Random random = new Random();
            if(random.nextInt(100)>90){
                //percent of loss
                log.warn("loss of task {}",taskTrigger.getId());
                return;
            }
            RunningTask runningTask = new RunningTask(taskTrigger);
            allTasks.putIfAbsent(taskTrigger.getId(), runningTask);
            occupyDevice(runningTask);
        });
    }

    private void occupyDevice(RunningTask runningTask) {
        Clazz deviceClass = runningTask.getTt().getDeviceClass();
        Optional<DeviceHolder> availabelDevice = deviceHolders.stream()
            .filter(deviceHolder ->deviceHolder.getDevice().getClazz() == deviceClass)
            .sorted(Comparator.comparingInt(dh -> dh.getWaitingQueue().size() + (dh.getTaskId()==null?0:1)))
            .findFirst();
        DeviceHolder deviceHolder = availabelDevice.orElseThrow();
        if(deviceHolder.getTaskId() != null){
            deviceHolder.getWaitingQueue().offer(runningTask);
        }else {
            deviceHolder.setTaskId(runningTask.getTt().getId());
            runningTask.deviceOccupied();
        }
    }

    private void cancel(List<Long> tasksToCancel) {
        tasksToCancel.forEach(tid->{
            allTasks.putIfAbsent(tid,new RunningTask(TaskTrigger.builder().id(tid).build()));
        });

        allTasks.entrySet().stream().filter(entry->tasksToCancel.contains(entry.getKey()))
            .map(Entry::getValue)
            .forEach(runningTask -> {
                runningTask.setStartTime(System.currentTimeMillis());
                runningTask.setStatus(TaskStatusInterface.CANCELING);
                runningTask.setCancel(true);
            });

    }

}
