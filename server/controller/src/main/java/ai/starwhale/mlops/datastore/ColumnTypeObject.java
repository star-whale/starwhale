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
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode(callSuper = false)
public class ColumnTypeObject extends ColumnType {

    private static final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile("^[\\p{Alnum}_]+$");

    public static final String TYPE_NAME = "OBJECT";

    private final String pythonType;

    private final Map<String, ColumnType> attributes;

    ColumnTypeObject(@NonNull String pythonType, @NonNull Map<String, ColumnType> attributes) {
        this.pythonType = pythonType;
        this.attributes = attributes;
        attributes.keySet().forEach(key -> {
            if (!ATTRIBUTE_NAME_PATTERN.matcher(key).matches()) {
                throw new IllegalArgumentException(
                        "invalid attribute name " + key + ". only alphabets, digits, and underscore are allowed");
            }
        });
    }

    @Override
    public String toString() {
        var ret = new StringBuilder(this.getPythonType());
        ret.append('{');
        var len = ret.length();
        attributes.forEach((key, value) -> {
            if (ret.length() > len) {
                ret.append(',');
            }
            ret.append(key);
            ret.append(':');
            ret.append(value);
        });
        ret.append('}');
        return ret.toString();
    }

    @Override
    public String getTypeName() {
        return ColumnTypeObject.TYPE_NAME;
    }

    @Override
    public ColumnSchemaDesc toColumnSchemaDesc(String name) {
        return ColumnSchemaDesc.builder()
                .name(name)
                .type(this.getTypeName())
                .pythonType(this.getPythonType())
                .attributes((this.attributes.entrySet().stream()
                        .map(entry -> entry.getValue().toColumnSchemaDesc(entry.getKey()))
                        .collect(Collectors.toList())))
                .build();
    }

    @Override
    public boolean isComparableWith(ColumnType other) {
        if (other == ColumnTypeScalar.UNKNOWN) {
            return true;
        }
        return other instanceof ColumnTypeObject
                && this.pythonType.equals(((ColumnTypeObject) other).pythonType)
                && this.attributes.equals(((ColumnTypeObject) other).attributes);
    }

    @Override
    public Object encode(Object value, boolean rawResult) {
        if (value == null) {
            return null;
        }
        //noinspection unchecked
        return ((Map<String, ?>) value).entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        entry -> Optional.ofNullable(this.attributes.get(entry.getKey()))
                                .orElseThrow(() -> new IllegalArgumentException("invalid attribute " + entry.getKey()))
                                .encode(entry.getValue(), rawResult)));
    }

    @Override
    public Object decode(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw new SwValidationException(ValidSubject.DATASTORE, "value should be of type Map");
        }
        //noinspection unchecked
        return ((Map<String, ?>) value).entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        entry -> Optional.ofNullable(this.attributes.get(entry.getKey()))
                                .orElseThrow(() -> new SwValidationException(ValidSubject.DATASTORE,
                                        "invalid attribute " + entry.getKey()))
                                .decode(entry.getValue())));
    }

    @Override
    public Object fromWal(Wal.Column col) {
        if (col.getNullValue()) {
            return null;
        }
        return col.getObjectValueMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        entry -> Optional.ofNullable(this.attributes.get(entry.getKey()))
                                .orElseThrow(() -> new IllegalArgumentException("invalid attribute " + entry.getKey()))
                                .fromWal(entry.getValue())));
    }

    @Override
    public Wal.Column.Builder toWal(int columnIndex, Object value) {
        var ret = Wal.Column.newBuilder().setIndex(columnIndex);
        if (value == null) {
            return ret.setNullValue(true);
        }
        //noinspection unchecked
        return ret.putAllObjectValue((((Map<String, ?>) value).entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        entry -> Optional.ofNullable(this.attributes.get(entry.getKey()))
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "invalid attribute " + entry.getKey()))
                                .toWal(0, entry.getValue())
                                .build()))));
    }
}
