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

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeleteRequest {
    @NotNull
    private String tableName;

    // encoded start key, null means start from the beginning
    private String startKey;
    // encoded end key, null means end at the end
    private String endKey;
    // keyType can not be null if startKey or endKey is not null
    private String keyType;

    @Builder.Default
    private Boolean startKeyInclusive = true;
    @Builder.Default
    private Boolean endKeyInclusive = false;

    // optional key prefix, only works if the keys in the table are strings and the keyPrefix is not null
    // keyPrefix and (start, end) can not be used together
    private String keyPrefix;
}

