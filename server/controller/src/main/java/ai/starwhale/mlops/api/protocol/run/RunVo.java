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

package ai.starwhale.mlops.api.protocol.run;

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunVo {

    private Long id;
    private Long taskId;
    private RunStatus status;
    private String ip;
    private Long startTime;
    private Long finishTime;
    private String failedReason;

    public RunVo(Run run) {
        if (null == run) {
            return;
        }
        this.id = run.getId();
        this.taskId = run.getTaskId();
        this.status = run.getStatus();
        this.ip = run.getIp();
        this.startTime = run.getStartTime();
        this.finishTime = run.getFinishTime();
        this.failedReason = run.getFailedReason();
    }
}
