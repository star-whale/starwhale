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

package ai.starwhale.mlops.datastore;

import ai.starwhale.mlops.datastore.Wal.Checkpoint.OptionalUserDataCase;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Checkpoint {
    private String userData;
    private long revision;
    private long timestamp;
    private long rowCount;

    // mark the first and the last checkpoints as virtual
    // prevent querying the garbage collected revisions
    @Builder.Default
    private boolean virtual = false;

    public static Checkpoint from(Wal.Checkpoint checkpoint) {
        String userData = null;
        if (checkpoint.getOptionalUserDataCase() == OptionalUserDataCase.USER_DATA) {
            userData = checkpoint.getUserData();
        }
        return Checkpoint.builder()
                .userData(userData)
                .revision(checkpoint.getRevision())
                .timestamp(checkpoint.getTimestamp())
                .rowCount(checkpoint.getRowCount())
                .build();
    }

    public Wal.Checkpoint toWalCheckpoint() {
        var cp = Wal.Checkpoint.newBuilder()
                .setRevision(revision)
                .setTimestamp(timestamp)
                .setRowCount(rowCount);
        if (userData != null) {
            cp.setUserData(userData);
        }
        return cp.build();
    }
}
