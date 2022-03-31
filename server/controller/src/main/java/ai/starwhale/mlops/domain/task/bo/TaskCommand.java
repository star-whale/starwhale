/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.domain.task.Task;
import ai.starwhale.mlops.domain.task.Task.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCommand {

    public enum CommandType{
        CANCEL(TaskStatus.CANCEL_COMMANDING),TRIGGER(TaskStatus.ASSIGNING),UNKNOWN(TaskStatus.UNKNOWN);
        final TaskStatus correspondStatus;
        CommandType(TaskStatus status){
            correspondStatus = status;
        }
        public TaskStatus getCorrespondStatus(){
            return this.correspondStatus;
        }
        public static CommandType from(TaskStatus status){
            for(CommandType commandType:CommandType.values()){
                if(commandType.correspondStatus == status){
                    return commandType;
                }
            }
            return UNKNOWN;
        }

    }

    CommandType commandType;

    Task task;

    public boolean agentProper(Task nodeTask){
        return null != nodeTask &&
            (this.commandType.correspondStatus.before(nodeTask.getStatus())
                || this.commandType.correspondStatus == nodeTask.getStatus());
    }

}
