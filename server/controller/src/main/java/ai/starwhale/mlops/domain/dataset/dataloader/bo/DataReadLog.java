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

package ai.starwhale.mlops.domain.dataset.dataloader.bo;

import ai.starwhale.mlops.domain.dataset.dataloader.Status;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataReadLog {

    private Long id;
    private Long sessionId;
    private String consumerId;
    private String start;
    @Builder.Default
    private boolean startInclusive = true;
    private String end;
    private boolean endInclusive;
    private int size;
    private int assignedNum;
    private Date assignedTime;
    @Builder.Default
    private Status.DataStatus status = Status.DataStatus.UNPROCESSED;
    private Date finishedTime;
    @Builder.Default
    private Date createdTime = new Date(-1);

}
