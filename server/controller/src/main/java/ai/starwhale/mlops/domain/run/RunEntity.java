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

package ai.starwhale.mlops.domain.run;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.domain.run.bo.RunSpec;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
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
public class RunEntity extends BaseEntity {

    private Long id;
    private Long taskId;
    private RunStatus status;
    private String logDir;
    private RunSpec runSpec;
    private String ip;
    String failedReason;
    private Date startTime;
    private Date finishTime;

}
