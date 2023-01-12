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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;
import org.springframework.util.StringUtils;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
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
    public String toString() {
        return "{alias:" + alias + ",path:" + path + ",type:" + type.toString() + "}";
    }

    public static boolean isVirtual(String pattern) {
        return Objects.nonNull(pattern) && pattern.startsWith("$.");
    }

    public Object parseFromValues(Map<String, Object> values) {
        var pair = NamePair.from(path);
        if (pair == null) {
            return null;
        }
        return getValue(pair, values);
    }

    private static Object getValue(NamePair pair, Object obj) {
        if (pair == null) {
            return obj;
        }

        Object subValue;
        if (obj instanceof Map) {
            subValue = ((Map<?, ?>) obj).get(pair.property);
            // special for list
            if (pair.index != null) {
                if (subValue instanceof List) {
                    subValue = pair.index < ((List<?>) subValue).size() ? ((List<?>) subValue).get(pair.index) : null;
                } else {
                    throw new UnsupportedOperationException("there needs list type but found " + obj.getClass());
                }
            }
        } else if (obj instanceof List) {
            if (pair.index != null) {
                subValue = pair.index < ((List<?>) obj).size() ? ((List<?>) obj).get(pair.index) : null;
            } else {
                throw new UnsupportedOperationException("no index for the list type " + obj.getClass());
            }
        } else {
            subValue = obj;
        }

        return getValue(NamePair.from(pair.subPath), subValue);
    }

    public static ColumnTypeVirtual build(
            String alias, String originPattern, Function<String, ColumnSchema> schemaSupplier) {
        if (!isVirtual(originPattern)) {
            return null;
        }
        var path = originPattern.substring(2);
        var pair = NamePair.from(path);
        var columnSchema = schemaSupplier.apply(Objects.requireNonNull(pair).property);
        if (columnSchema == null) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "virtual column name " + alias + " not found");
        }
        var columnType = parseTypeFromParent(pair, columnSchema.getType());

        return columnType == null ? null : new ColumnTypeVirtual(alias, path, columnType);
    }

    private static ColumnType parseTypeFromParent(NamePair parentPair, ColumnType parentColumnType) {
        NamePair subPair = NamePair.from(parentPair.subPath);

        if (subPair != null) {
            ColumnType subColumnType;
            if (parentPair.index != null) {
                if (parentColumnType instanceof ColumnTypeList) {
                    subColumnType = ((ColumnTypeList) parentColumnType).getElementType();
                    // need parse more than 1 time
                    subPair = NamePair.of(parentPair.property, null, parentPair.subPath);
                } else {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "virtual column parse error for " + parentPair
                            + ",can't support type:" + parentColumnType.getClass());
                }
            } else {
                if (parentColumnType instanceof ColumnTypeObject) {
                    subColumnType = ((ColumnTypeObject) parentColumnType).getAttributes().get(
                        Objects.requireNonNull(subPair).property);
                } else if (parentColumnType instanceof ColumnTypeMap) {
                    subColumnType = ((ColumnTypeMap) parentColumnType).getValueType();
                } else if (parentColumnType instanceof ColumnTypeScalar) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "virtual column parse error for " + parentPair + ", because the type haven't more sub types");
                } else {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "virtual column parse error for " + parentPair
                            + ",can't support type:" + parentColumnType.getClass());
                }
            }
            return parseTypeFromParent(subPair, subColumnType);
        } else {
            // directly return column type
            if (parentPair.index != null) {
                if (parentColumnType instanceof ColumnTypeList) {
                    return ((ColumnTypeList) parentColumnType).getElementType();
                } else {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "virtual column parse error for " + parentPair
                            + ",can't support type:" + parentColumnType.getClass());
                }
            } else {
                return parentColumnType;
            }
        }
    }

    @Getter
    @Value(staticConstructor = "of")
    private static class NamePair {
        String property;
        Integer index;
        String subPath;

        public String toString() {
            return "{property:" + property + "index:" + index + ",subPath:" + subPath + "}";
        }

        private static NamePair from(String path) {
            if (!StringUtils.hasText(path)) {
                return null;
            }
            var index = path.indexOf(".");
            if (index == -1) {
                return NamePair.of(getProperty(path), getIndex(path), null);
            } else {
                var property = path.substring(0, index);
                var subPath = path.substring(index + 1);
                return NamePair.of(getProperty(property), getIndex(property), subPath);
            }
        }

        private static String getProperty(String data) {
            if (data.contains("[") && data.contains("]")) {
                return data.substring(0, data.indexOf("["));
            } else {
                return data;
            }
        }

        private static Integer getIndex(String data) {
            List<String> list = new ArrayList<>();
            Pattern p = Pattern.compile("(\\[[^\\]]*\\])");
            Matcher m = p.matcher(data);
            while (m.find()) {
                list.add(m.group().substring(1, m.group().length() - 1));
            }
            if (list.isEmpty()) {
                return null;
            } else {
                Optional<Integer> index = list.stream()
                        .filter(ColumnTypeVirtual.NamePair::isInteger)
                        .map(Integer::valueOf)
                        .findFirst();
                return index.orElse(null);
            }
        }

        private static boolean isInteger(String s) {
            return isPositiveInteger(s, 10);
        }

        private static boolean isPositiveInteger(String s, int radix) {
            if (s.isEmpty()) {
                return false;
            }
            for (int i = 0; i < s.length(); i++) {
                if (i == 0 && s.charAt(i) == '-') {
                    return false;
                }
                if (Character.digit(s.charAt(i), radix) < 0) {
                    return false;
                }
            }
            return true;
        }
    }


    @Override
    public ColumnSchemaDesc toColumnSchemaDesc(String name) {
        // name == alias
        return ColumnSchemaDesc.builder()
                .name(name)
                .type(TYPE_NAME)
                .origin(path)
                .elementType(type.toColumnSchemaDesc(null))
                .build();
    }

    @Override
    public boolean isComparableWith(ColumnType other) {
        return false;
    }

    @Override
    public Object encode(Object value, boolean rawResult) {
        return type.encode(value, rawResult);
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
