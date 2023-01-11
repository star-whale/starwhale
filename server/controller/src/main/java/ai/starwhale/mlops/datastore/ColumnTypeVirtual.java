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

import ai.starwhale.mlops.datastore.parquet.ValueSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;

@Getter
@NoArgsConstructor
public class ColumnTypeVirtual extends ColumnType {
    public static final String TYPE_NAME = "VIRTUAL";

    /**
     * The alias name of the column.
     */
    private String alias;

    /**
     * The pattern name of the column.
     */
    private String path;

    private ColumnType type;

    public ColumnTypeVirtual(String alias, String path, ColumnType type) {
        this.alias = alias;
        this.path = path;
        this.type = type;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public ColumnSchemaDesc toColumnSchemaDesc(String name) {
        // name == alias
        return ColumnSchemaDesc.builder().name(name).origin(path).type(TYPE_NAME).build();
    }

    @Override
    public boolean isComparableWith(ColumnType other) {
        return false;
    }

    @Override
    public Object encode(Object value, boolean rawResult) {
        // TODO support it when parse data at querying
        throw new UnsupportedOperationException("can't support encode");
    }

    @Override
    public Object decode(Object value) {
        throw new UnsupportedOperationException("can't support decode");
    }

    @Override
    public void fillWalColumnSchema(Wal.ColumnSchema.Builder builder) {
        throw new UnsupportedOperationException("can't support fillWalColumnSchema operation");
    }

    @Override
    public Object fromWal(Wal.Column col) {
        throw new UnsupportedOperationException("can't support fromWal operation");
    }

    @Override
    public Wal.Column.Builder toWal(int columnIndex, Object value) {
        throw new UnsupportedOperationException("can't support toWal operation");
    }

    @Override
    protected Types.Builder<?, ? extends Type> buildParquetType() {
        throw new UnsupportedOperationException("can't support buildParquet operation");
    }

    @Override
    protected void writeNonNullParquetValue(RecordConsumer recordConsumer, @NonNull Object value) {
        throw new UnsupportedOperationException("can't support writeNonNullParquetValue operation");
    }

    @Override
    protected Converter getParquetValueConverter(ValueSetter valueSetter) {
        throw new UnsupportedOperationException("can't support getParquetValueConverter operation");
    }
}
