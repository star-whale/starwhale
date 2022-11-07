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
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;

@Getter
@EqualsAndHashCode(callSuper = false)
public class ColumnTypeList extends ColumnType {

    public static final String TYPE_NAME = "LIST";

    private final ColumnType elementType;

    ColumnTypeList(ColumnType elementType) {
        this.elementType = elementType;
    }

    @Override
    public String toString() {
        return "[" + elementType + "]";
    }

    @Override
    public String getTypeName() {
        return ColumnTypeList.TYPE_NAME;
    }

    @Override
    public ColumnSchemaDesc toColumnSchemaDesc(String name) {
        return ColumnSchemaDesc.builder()
                .name(name)
                .type(this.getTypeName())
                .elementType(this.getElementType().toColumnSchemaDesc(null))
                .build();
    }

    @Override
    public boolean isComparableWith(ColumnType other) {
        if (other == ColumnTypeScalar.UNKNOWN) {
            return true;
        }
        return other instanceof ColumnTypeList
                && this.elementType.isComparableWith(((ColumnTypeList) other).elementType);
    }

    @Override
    public Object encode(Object value, boolean rawResult) {
        if (value == null) {
            return null;
        }
        return ((List<?>) value).stream()
                .map(element -> this.elementType.encode(element, rawResult))
                .collect(Collectors.toList());
    }

    @Override
    public Object decode(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new SwValidationException(ValidSubject.DATASTORE, "value should be of type List");
        }
        return ((List<?>) value).stream()
                .map(this.elementType::decode)
                .collect(Collectors.toList());
    }

    @Override
    public Object fromWal(Wal.Column col) {
        if (col.getNullValue()) {
            return null;
        }
        return col.getListValueList().stream()
                .map(this.elementType::fromWal)
                .collect(Collectors.toList());
    }

    @Override
    public Wal.Column.Builder toWal(int columnIndex, Object value) {
        var ret = Wal.Column.newBuilder().setIndex(columnIndex);
        if (value == null) {
            return ret.setNullValue(true);
        }
        return ret.addAllListValue(((List<?>) value).stream()
                .map(element -> this.elementType.toWal(0, element).build())
                .collect(Collectors.toList()));
    }

    @Override
    public Types.Builder<?, ? extends Type> buildParquetType() {
        return Types.optionalGroup().addField(
                Types.repeatedGroup().addField(elementType.toParquetType("element")).named("list"));
    }

    @Override
    public void writeNonNullParquetValue(RecordConsumer recordConsumer, @NonNull Object value) {
        recordConsumer.startField("list", 0);
        var listValue = (List<?>) value;
        for (var element : listValue) {
            recordConsumer.startGroup();
            recordConsumer.startField("element", 0);
            this.elementType.writeParquetValue(recordConsumer, element);
            recordConsumer.endField("element", 0);
            recordConsumer.endGroup();
        }
        recordConsumer.endField("list", 0);
    }

    @Override
    protected Converter getParquetValueConverter(ValueSetter valueSetter) {
        var list = new AtomicReference<List<Object>>();
        var elementConverter = elementType.getParquetConverter(v -> {
            list.get().add(v);
        });
        var listConverter = new GroupConverter() {
            @Override
            public Converter getConverter(int i) {
                return elementConverter;
            }

            @Override
            public void start() {
            }

            @Override
            public void end() {
            }
        };
        return new GroupConverter() {
            @Override
            public Converter getConverter(int i) {
                return listConverter;
            }

            @Override
            public void start() {
                list.set(new ArrayList<>());
            }

            @Override
            public void end() {
                valueSetter.setValue(list.get());
            }
        };
    }
}
