/*
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
