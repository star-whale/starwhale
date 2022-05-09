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

package ai.starwhale.mlops.agent.task.inferencetask;

import ai.starwhale.mlops.api.protocol.TaskStatusInterface;
import ai.starwhale.mlops.api.protocol.report.req.TaskLog;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import ai.starwhale.mlops.api.protocol.report.resp.SWDSBlockVO;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.TaskType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * sufficient information for an Agent to run a Task
 */
@Data
@Builder
public class InferenceTask {

    /**
     * unique id for the task
     */
    Long id;

    /**
     * ppl or resulting
     */
    TaskType taskType;

    /**
     * the proper image to get swmp run
     */
    String imageId;

    /**
     * swmp meta info
     */
    SWModelPackage swModelPackage;

    /**
     * task status
     */
    InferenceTaskStatus status;

    /**
     * runtime information at all stage: the container id、the devices list which the task hold
     */
    String containerId;

    int retryRunNum;
    public void retryRun() {
        retryRunNum++;
    }
    int retryRestartNum;
    public void retryRestart() {
        retryRestartNum ++;
    }

    /**
     * runtime information at ppl stage: devices allocated by agent
     */
    Set<Device> devices;

    /**
     * input information at resulting stage: CMP file path
     */
    List<String> cmpInputFilePaths;

    /**
     * input information at ppl stage: SWDS(blocks may come from different SWDS)、device info
     */
    List<SWDSBlockVO> swdsBlocks;

    Integer deviceAmount;

    Device.Clazz deviceClass;

    /**
     * output information at the end of stage: the single task's result
     */
    ResultPath resultPath;

    /**
     * task's execute stage
     */
    InferenceStage stage;

    /**
     * every action will have a status which represents completion
     */
    ActionStatus actionStatus;

    public enum ActionStatus {
        inProgress, completed
    }

    public boolean equals(Object obj) {

        if (!(obj instanceof InferenceTask)) {
            return false;
        }
        InferenceTask tt = (InferenceTask) obj;
        return this.getId().equals(tt.getId());
    }

    public static InferenceTask fromTaskTrigger(TaskTrigger taskTrigger) {
        return InferenceTask.builder().id(taskTrigger.getId())
                .imageId(taskTrigger.getImageId())
                .taskType(taskTrigger.getTaskType())
                .status(InferenceTaskStatus.PREPARING)
                .actionStatus(ActionStatus.inProgress)
                .cmpInputFilePaths(taskTrigger.getCmpInputFilePaths())
                .deviceAmount(taskTrigger.getDeviceAmount())
                .deviceClass(taskTrigger.getDeviceClass())
                .swdsBlocks(taskTrigger.getSwdsBlocks())
                .resultPath(taskTrigger.getResultPath())
                .swModelPackage(taskTrigger.getSwModelPackage())
                .build();
    }


    public TaskReport toTaskReport(List<TaskLog> logs) {
        TaskStatusInterface reportStatus = null;
        switch (this.status) {
            case PREPARING:
            case RUNNING:
            case UPLOADING:
                reportStatus = TaskStatusInterface.RUNNING;
                break;
            case SUCCESS:
            case ARCHIVED:
                reportStatus = TaskStatusInterface.SUCCESS;
                break;
            case FAIL:
                reportStatus = TaskStatusInterface.FAIL;
                break;
            case CANCELING:
                reportStatus = TaskStatusInterface.CANCELING;
                break;
            case CANCELED:
                reportStatus = TaskStatusInterface.CANCELED;
                break;
        }
        return TaskReport.builder().id(this.id).readerLogs(logs).status(reportStatus).taskType(this.taskType).build();
    }
}
