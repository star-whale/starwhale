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

package ai.starwhale.mlops.domain.dataset.dataloader.po;

import ai.starwhale.mlops.domain.dataset.dataloader.Status;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEntity {
    private Long id;
    private String sessionId;
    private int batchSize;
    private String datasetName;
    private String datasetVersion;
    private String tableName;
    private String current;
    private boolean currentInclusive;

    private String start;
    private String startType;
    private boolean startInclusive;
    private String end;
    private String endType;
    private boolean endInclusive;

    private Status.SessionStatus status;

    private Date createdTime;
}
