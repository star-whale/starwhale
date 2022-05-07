/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCommand {

    public enum CommandType{
        CANCEL(TaskStatus.CANCELLING),TRIGGER(TaskStatus.ASSIGNING),UNKNOWN(TaskStatus.UNKNOWN);
        final TaskStatus correspondStatus;
        CommandType(TaskStatus status){
            correspondStatus = status;
        }

        public TaskStatus getCorrespondStatus() {
            return correspondStatus;
        }

        public static CommandType from(TaskStatus status){
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskCommand that = (TaskCommand) o;
        return task.equals(that.task);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task);
    }
}
