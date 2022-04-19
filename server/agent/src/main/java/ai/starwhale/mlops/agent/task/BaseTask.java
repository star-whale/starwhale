/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task;

import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.swmp.SWModelPackage;
import ai.starwhale.mlops.domain.task.TaskStatus;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Set;

/**
 * sufficient information for an Agent to run a Task
 */
@Data
@SuperBuilder
public class BaseTask {

    /**
     * unique id for the task
     */
    Long id;

    /**
     * the proper image to get swmp run
     */
    String imageId;


    /**
     * pre-specified path
     */
    String resultPath;

    /**
     * swmp meta info
     */
    SWModelPackage swModelPackage;

    Integer deviceAmount;

    Device.Clazz deviceClass;


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
     * every stage will have a status which represents completion
     */
    Stage stage;

    public enum Stage {
        inProgress, completed
    }

    public boolean equals(Object obj) {

        if (!(obj instanceof BaseTask)) {
            return false;
        }
        BaseTask tt = (BaseTask) obj;
        return this.getId().equals(tt.getId());
    }

    public TaskReport toTaskReport() {
        return TaskReport.builder().id(this.id).status(this.status).build();
    }
}
