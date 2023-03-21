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

import java.util.Arrays;
import java.util.Comparator;
import lombok.Getter;

@Getter
public enum ColumnType {
    UNKNOWN(0),
    BOOL(1),
    INT8(2),
    INT16(3),
    INT32(4),
    INT64(5),
    FLOAT32(6),
    FLOAT64(7),
    STRING(8),
    BYTES(9),
    LIST(10),
    TUPLE(11),
    MAP(12),
    OBJECT(13);

    private final int index;
    private final String category;
    private final int nbits;
    private static final ColumnType[] TYPES = Arrays.stream(ColumnType.values())
            .sorted(Comparator.comparingInt(a -> a.index))
            .toArray(ColumnType[]::new);

    ColumnType(int index) {
        this.index = index;
        if (this.name().startsWith("INT")) {
            this.category = "INT";
        } else if (this.name().startsWith("FLOAT")) {
            this.category = "FLOAT";
        } else {
            this.category = null;
        }
        if (this.category != null) {
            this.nbits = Integer.parseInt(this.name().substring(this.category.length()));
        } else {
            this.nbits = 0;
        }
    }

    public static ColumnType getTypeByIndex(int index) {
        return TYPES[index];
    }
}
