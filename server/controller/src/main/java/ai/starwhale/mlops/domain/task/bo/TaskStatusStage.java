/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

public enum TaskStatusStage {
    INIT(0x1),DOING(0x2),DONE(0x9),FAILED(0x0);
    int value;
    TaskStatusStage(int value){
        this.value = value;
    }

    public static TaskStatusStage from(int v){
        for(TaskStatusStage taskStatusStage:TaskStatusStage.values()){
            if(taskStatusStage.value == v){
                return taskStatusStage;
            }
        }
        return INIT;
    }

    public int getValue() {
        return value;
    }
}
