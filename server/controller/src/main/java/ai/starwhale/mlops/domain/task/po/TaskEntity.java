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

package ai.starwhale.mlops.domain.task.po;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity extends BaseEntity {

    private Long id;

    private String taskUuid;

    private Long stepId;

    private Integer retryNum;

    private TaskStatus taskStatus;

    private String outputPath;

    private String taskRequest;

    private String ip;

    private String devPassword;

    private DevWay devWay;

    private Date startedTime;

    private Date finishedTime;

}
