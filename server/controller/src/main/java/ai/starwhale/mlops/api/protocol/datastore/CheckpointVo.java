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

package ai.starwhale.mlops.api.protocol.datastore;

import ai.starwhale.mlops.datastore.Checkpoint;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckpointVo {
    @NotNull
    private String id;

    @NotNull
    private Long createdTime;

    @NotNull
    private Long rowCount;

    private String userData;

    public static CheckpointVo from(Checkpoint checkpoint) {
        return CheckpointVo.builder()
                .id(String.valueOf(checkpoint.getRevision()))
                .createdTime(checkpoint.getTimestamp())
                .userData(checkpoint.getUserData())
                .rowCount(checkpoint.getRowCount())
                .build();
    }
}
