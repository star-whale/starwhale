/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.domain.task.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCommand {

    public enum CommandType{
        CANCEL(new StagingTaskStatus(TaskStatus.CANCEL,TaskStatusStage.DOING)),TRIGGER(new StagingTaskStatus(TaskStatus.CREATED,TaskStatusStage.DOING)),UNKNOWN(new StagingTaskStatus(TaskStatus.UNKNOWN));
        final StagingTaskStatus correspondStatus;
        CommandType(StagingTaskStatus status){
            correspondStatus = status;
        }
        public StagingTaskStatus getCorrespondStatus(){
            return this.correspondStatus;
        }
        public static CommandType from(StagingTaskStatus status){
            for(CommandType commandType:CommandType.values()){
                if(commandType.correspondStatus.equals(status)){
                    return commandType;
                }
            }
            return UNKNOWN;
        }

    }

    CommandType commandType;

    Task task;

    public boolean agentProper(Task nodeTask){
        return null != nodeTask
            && this.commandType.correspondStatus.clearStage().compareTo(nodeTask.getStatus()) <= 0;
    }

}
