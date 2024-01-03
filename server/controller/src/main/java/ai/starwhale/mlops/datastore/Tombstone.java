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

import ai.starwhale.mlops.datastore.impl.WalRecordDecoder;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.StringValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tombstone {
    // notice that the start and end must be the same type
    private BaseValue start;
    private BaseValue end;
    private boolean startInclusive;
    private boolean endInclusive;
    private String keyPrefix;

    public static Tombstone from(Wal.Tombstone tombstone) {
        var ret = Tombstone.builder();
        if (tombstone.hasPrefix()) {
            ret.keyPrefix(tombstone.getPrefix().getKeyPrefix());
            return ret.build();
        }

        if (tombstone.hasRange()) {
            var range = tombstone.getRange();
            ret.start(WalRecordDecoder.decodeValue(range.getStartKey()));
            ret.end(WalRecordDecoder.decodeValue(range.getEndKey()));
            ret.startInclusive(range.getStartInclusive());
            ret.endInclusive(range.getEndInclusive());
            return ret.build();
        }
        throw new IllegalArgumentException("invalid tombstone");
    }

    /**
     * check if the key is deleted by this tombstone
     * <p>
     * if the keyPrefix is set, then the key is deleted if it starts with the prefix
     * if the start and end are set, then the key is deleted if it is in the range
     * the key type must be the same as the start and end
     * if the start or end is null, then the range is unbounded
     * </p>
     *
     * @param key the key to check
     * @return true if the key is deleted by this tombstone
     */
    public boolean keyDeleted(@NonNull BaseValue key) {
        if (keyPrefix != null) {
            // check if key is StringValue
            if (!(key instanceof StringValue)) {
                return false;
            }
            var keyStr = ((StringValue) key).getValue();
            if (keyStr == null) {
                return false;
            }
            return keyStr.startsWith(keyPrefix);
        }
        if (start == null && end == null) {
            return true;
        }

        var rangeType = start != null ? start.getClass() : end.getClass();
        if (!rangeType.isInstance(key)) {
            return false;
        }

        if (start != null && end != null) {
            var cmpStart = start.compareTo(key);
            var cmpEnd = end.compareTo(key);
            if (cmpStart < 0 || (cmpStart == 0 && startInclusive)) {
                return cmpEnd > 0 || (cmpEnd == 0 && endInclusive);
            }
        } else {
            if (start != null) {
                var cmpStart = start.compareTo(key);
                if (cmpStart < 0 || (cmpStart == 0 && startInclusive)) {
                    return true;
                }
            }
            if (end != null) {
                var cmpEnd = end.compareTo(key);
                return cmpEnd > 0 || (cmpEnd == 0 && endInclusive);
            }
        }
        return false;
    }
}
