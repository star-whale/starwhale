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

import ai.starwhale.mlops.common.TimeConcern;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Tasks are derived from a Job. Tasks are the executing units of a Job.
 */
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Builder
@Getter
public class Task extends TimeConcern {

    /**
     * unique id for the task
     */
    Long id;

    /**
     * random uuid for the task
     */
    String uuid;

    /**
     * status of the task
     */
    TaskStatus status;

    Integer retryNum;

    /**
     * storage directory path of results
     */
    ResultPath resultRootPath;

    TaskRequest taskRequest;

    /**
     * the step where the task is derived from
     */
    Step step;

    String ip;
    DevWay devWay;

    public void updateStatus(TaskStatus status) {
        this.status = status;
    }

    public void setRetryNum(Integer retryNum) {
        this.retryNum = retryNum;
    }

    public void setResultRootPath(ResultPath resultRootPath) {
        this.resultRootPath = resultRootPath;
    }

    public void setTaskRequest(TaskRequest taskRequest) {
        this.taskRequest = taskRequest;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Task)) {
            return false;
        }
        Task tsk = (Task) obj;
        return this.uuid.equals(tsk.uuid);
    }

}
