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

package ai.starwhale.mlops.datastore.backup;

import ai.starwhale.mlops.api.protocol.datastore.BackupVo;
import ai.starwhale.mlops.common.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meta {
    /**
     * The id of the backup, unique
     */
    String id;

    /**
     * The time when the backup is created
     */
    private Long createdAt;

    /**
     * The time when the backup is done
     */
    private Long doneAt;

    /**
     * The approximate size of the backup in bytes
     */
    private Long approximateSizeBytes;

    /**
     * The snapshots in the shared folder, which are shared by multiple backups
     */
    private List<Snapshot> snapshots;

    public static Meta fromInputStream(InputStream inputStream) throws IOException {
        return Constants.objectMapper.readValue(inputStream, Meta.class);
    }

    public byte[] toBytes() throws IOException {
        return Constants.objectMapper.writeValueAsBytes(this);
    }

    public BackupVo toVo() {
        return BackupVo.builder()
                .id(id)
                .createdAt(createdAt)
                .doneAt(doneAt)
                .approximateSizeBytes(approximateSizeBytes)
                .build();
    }
}
