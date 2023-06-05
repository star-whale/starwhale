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

package ai.starwhale.mlops.domain.job.step.po;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = false)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StepEntity extends BaseEntity {

    String name;

    Long id;

    Long lastStepId;

    Long jobId;

    StepStatus status;

    String uuid;

    Integer concurrency = 1;

    // private List<Resource> resources;

    Integer taskNum = 1;

    String poolInfo;

    @Builder.Default
    Date startedTime = defaultDate;

    @Builder.Default
    Date finishedTime = defaultDate;
}
