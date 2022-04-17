/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task;

import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.api.protocol.report.resp.TaskTrigger;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.swds.index.SWDSBlock;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.TaskStatus;

import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * sufficient information for an Agent to run a Task
 */
@Data
@Builder
public class EvaluationTask {

    /**
     * unique id for the task
     */
    Long id;

    /**
     * the proper image to get swmp run
     */
    String imageId;

    String resultPath;

    /**
     * task status
     */
    TaskStatus status;

    /**
     * the container id
     */
    String containerId;

    /**
     * the devices list which the task hold
     */
    Set<Device> devices;

    /**
     * swmp meta info
     */
    SWModelPackage swModelPackage;

    /**
     * blocks may come from different SWDS
     */
    List<SWDSBlock> swdsBlocks;

    Integer deviceAmount;

    Device.Clazz deviceClass;

    /**
     * every stage will have a status which represents completion
     */
    Stage stage;

    public enum Stage {
        inProgress, completed
    }

    public boolean equals(Object obj) {

        if (!(obj instanceof EvaluationTask)) {
            return false;
        }
        EvaluationTask tt = (EvaluationTask) obj;
        return this.getId().equals(tt.getId());
    }

    public static EvaluationTask fromTaskTrigger(TaskTrigger taskTrigger) {
        return EvaluationTask.builder().id(taskTrigger.getId())
                .imageId(taskTrigger.getImageId())
                .resultPath(taskTrigger.getResultPath())
                .status(TaskStatus.CREATED)
                .stage(Stage.inProgress)
                .deviceAmount(taskTrigger.getDeviceAmount())
                .deviceClass(taskTrigger.getDeviceClass())
                .swdsBlocks(taskTrigger.getSwdsBlocks())
                .swModelPackage(taskTrigger.getSwModelPackage())
                .build();
    }


    public TaskReport toTaskReport() {
        return TaskReport.builder().id(this.id).status(this.status).build();
    }
}
