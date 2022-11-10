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

import ai.starwhale.mlops.exception.SwValidationException;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class ColumnSchema {

    private static final Pattern COLUMN_NAME_PATTERN =
            Pattern.compile("^[\\p{Alnum}-_/: ]*$");

    private final String name;
    private final ColumnType type;
    private final int index;

    public ColumnSchema(@NonNull ColumnSchemaDesc schema, int index) {
        this.name = schema.getName();
        if (this.name == null) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE).tip(
                    "column name should not be null");
        }
        if (!ColumnSchema.COLUMN_NAME_PATTERN.matcher(this.name).matches()) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "invalid column name " + this.name + ". only alphabets, digits, hyphen(-), underscore(_), "
                            + "slash(/), colon(:), and space are allowed.");
        }
        if (schema.getType() == null) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE).tip(
                    "column type should not be null");
        }
        try {
            this.type = ColumnType.fromColumnSchemaDesc(schema);
        } catch (IllegalArgumentException e) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "invalid column schema: " + schema,
                    e);
        }
        this.index = index;
    }

    public ColumnSchema(Wal.ColumnSchema schema) {
        this(WalManager.parseColumnSchema(schema), schema.getColumnIndex());
    }
}
