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

package ai.starwhale.mlops.datastore.parquet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import ai.starwhale.mlops.datastore.ColumnSchema;
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ParquetConfig;
import ai.starwhale.mlops.datastore.ParquetConfig.CompressionCodec;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.ObjectValue;
import ai.starwhale.mlops.datastore.type.TupleValue;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParquetReadWriteTest {

    private StorageAccessService storageAccessService;

    @BeforeEach
    public void setUp() {
        this.storageAccessService = new StorageAccessServiceMemory();
    }

    @Test
    public void testReadWrite() throws Exception {
        var schema = new TableSchema(new TableSchemaDesc("string", List.of(
                ColumnSchemaDesc.builder().name("string").type("STRING").build(),
                ColumnSchemaDesc.builder().name("bool").type("BOOL").build(),
                ColumnSchemaDesc.builder().name("int8").type("INT8").build(),
                ColumnSchemaDesc.builder().name("int16").type("INT16").build(),
                ColumnSchemaDesc.builder().name("int32").type("INT32").build(),
                ColumnSchemaDesc.builder().name("int64").type("INT64").build(),
                ColumnSchemaDesc.builder().name("float32").type("FLOAT32").build(),
                ColumnSchemaDesc.builder().name("float64").type("FLOAT64").build(),
                ColumnSchemaDesc.builder().name("bytes").type("BYTES").build(),
                ColumnSchemaDesc.builder().name("unknown").type("UNKNOWN").build(),
                ColumnSchemaDesc.builder().name("list")
                        .type("LIST")
                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                        .build(),
                ColumnSchemaDesc.builder().name("list_object")
                        .type("LIST")
                        .elementType((ColumnSchemaDesc.builder()
                                .type("OBJECT")
                                .pythonType("placeholder")
                                .attributes(List.of(
                                        ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                        ColumnSchemaDesc.builder().name("b").type("STRING").build()
                                ))
                                .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("list_list")
                        .type("LIST")
                        .elementType((ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("list_tuple")
                        .type("LIST")
                        .elementType((ColumnSchemaDesc.builder()
                                .type("TUPLE")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("list_map")
                        .type("LIST")
                        .elementType((ColumnSchemaDesc.builder()
                                .type("MAP")
                                .keyType(ColumnSchemaDesc.builder().type("STRING").build())
                                .valueType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("tuple")
                        .type("TUPLE")
                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                        .build(),
                ColumnSchemaDesc.builder().name("tuple_object")
                        .type("TUPLE")
                        .elementType((ColumnSchemaDesc.builder()
                                .type("OBJECT")
                                .pythonType("placeholder")
                                .attributes(List.of(
                                        ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                        ColumnSchemaDesc.builder().name("b").type("STRING").build()
                                ))
                                .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("tuple_list")
                        .type("TUPLE")
                        .elementType((ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("tuple_tuple")
                        .type("TUPLE")
                        .elementType((ColumnSchemaDesc.builder()
                                .type("TUPLE")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("tuple_map")
                        .type("TUPLE")
                        .elementType((ColumnSchemaDesc.builder()
                                .type("MAP")
                                .keyType(ColumnSchemaDesc.builder().type("STRING").build())
                                .valueType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("map")
                        .type("MAP")
                        .keyType(ColumnSchemaDesc.builder().type("STRING").build())
                        .valueType(ColumnSchemaDesc.builder().type("INT32").build())
                        .build(),
                ColumnSchemaDesc.builder().name("map_object")
                        .type("MAP")
                        .keyType(ColumnSchemaDesc.builder()
                                .type("OBJECT")
                                .pythonType("placeholder")
                                .attributes(List.of(
                                        ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                        ColumnSchemaDesc.builder().name("b").type("STRING").build()
                                ))
                                .build())
                        .valueType(ColumnSchemaDesc.builder()
                                .type("OBJECT")
                                .pythonType("placeholder")
                                .attributes(List.of(
                                        ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                        ColumnSchemaDesc.builder().name("b").type("STRING").build()
                                ))
                                .build())
                        .build(),
                ColumnSchemaDesc.builder().name("map_list")
                        .type("MAP")
                        .keyType(ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())
                        .valueType(ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())
                        .build(),
                ColumnSchemaDesc.builder().name("map_tuple")
                        .type("MAP")
                        .keyType(ColumnSchemaDesc.builder()
                                .type("TUPLE")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())
                        .valueType(ColumnSchemaDesc.builder()
                                .type("TUPLE")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())
                        .build(),
                ColumnSchemaDesc.builder().name("map_map")
                        .type("MAP")
                        .keyType(ColumnSchemaDesc.builder()
                                .type("MAP")
                                .keyType(ColumnSchemaDesc.builder().type("STRING").build())
                                .valueType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())
                        .valueType(ColumnSchemaDesc.builder()
                                .type("MAP")
                                .keyType(ColumnSchemaDesc.builder().type("STRING").build())
                                .valueType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())
                        .build(),
                ColumnSchemaDesc.builder().name("object")
                        .type("OBJECT")
                        .pythonType("placeholder")
                        .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("b").type("STRING").build()))
                        .build(),
                ColumnSchemaDesc.builder().name("object_object")
                        .type("OBJECT")
                        .pythonType("placeholder")
                        .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("b")
                                        .type("OBJECT")
                                        .pythonType("placeholder")
                                        .attributes(List.of(
                                                ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                                ColumnSchemaDesc.builder().name("b").type("STRING").build()
                                        ))
                                        .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("object_list")
                        .type("OBJECT")
                        .pythonType("placeholder")
                        .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("b")
                                        .type("LIST")
                                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                        .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("object_tuple")
                        .type("OBJECT")
                        .pythonType("placeholder")
                        .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("b")
                                        .type("TUPLE")
                                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                        .build()))
                        .build(),
                ColumnSchemaDesc.builder().name("object_map")
                        .type("OBJECT")
                        .pythonType("placeholder")
                        .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("b")
                                        .type("MAP")
                                        .keyType(ColumnSchemaDesc.builder().type("STRING").build())
                                        .valueType(ColumnSchemaDesc.builder().type("INT32").build())
                                        .build()))
                        .build())));
        var parquetConfig = new ParquetConfig();
        parquetConfig.setCompressionCodec(CompressionCodec.SNAPPY);
        parquetConfig.setRowGroupSize(1024 * 1024);
        parquetConfig.setPageSize(4096);
        parquetConfig.setPageRowCountLimit(1000);
        List<Map<String, BaseValue>> records = List.of(
                new TreeMap<>() {
                    { // values that match the schema
                        put("string", BaseValue.valueOf("0"));
                        put("bool", BaseValue.valueOf(true));
                        put("int8", BaseValue.valueOf((byte) 2));
                        put("int16", BaseValue.valueOf((short) 3));
                        put("int32", BaseValue.valueOf(4));
                        put("int64", BaseValue.valueOf(5L));
                        put("float32", BaseValue.valueOf(1.1f));
                        put("float64", BaseValue.valueOf(1.2));
                        put("bytes", BaseValue.valueOf(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))));
                        put("unknown", null);
                        put("list", BaseValue.valueOf(List.of(1, 2, 3)));
                        put("list_object", BaseValue.valueOf(List.of(
                                ObjectValue.valueOf("v", Map.of("b", "11", "a", 21)),
                                ObjectValue.valueOf("v", Map.of("b", "12", "a", 22)),
                                ObjectValue.valueOf("v", Map.of("b", "13", "a", 23)))));
                        put("list_list", BaseValue.valueOf(List.of(
                                BaseValue.valueOf(List.of(1, 2)),
                                BaseValue.valueOf(List.of(3, 4, 5)),
                                BaseValue.valueOf(List.of(6)))));
                        put("list_tuple", BaseValue.valueOf(List.of(
                                TupleValue.valueOf(List.of(1, 2)),
                                TupleValue.valueOf(List.of(3, 4, 5)),
                                TupleValue.valueOf(List.of(6)))));
                        put("list_map", BaseValue.valueOf(List.of(
                                BaseValue.valueOf(Map.of("a", 0, "b", 1)),
                                BaseValue.valueOf(Map.of("a", 1, "b", 2)),
                                BaseValue.valueOf(Map.of("a", 2, "b", 3)))));
                        put("tuple", TupleValue.valueOf(List.of(1, 2, 3)));
                        put("tuple_object", TupleValue.valueOf(List.of(
                                ObjectValue.valueOf("v", Map.of("b", "11", "a", 21)),
                                ObjectValue.valueOf("v", Map.of("b", "12", "a", 22)),
                                ObjectValue.valueOf("v", Map.of("b", "13", "a", 23)))));
                        put("tuple_list", TupleValue.valueOf(List.of(
                                BaseValue.valueOf(List.of(1, 2)),
                                BaseValue.valueOf(List.of(3, 4, 5)),
                                BaseValue.valueOf(List.of(6)))));
                        put("tuple_tuple", TupleValue.valueOf(List.of(
                                TupleValue.valueOf(List.of(1, 2)),
                                TupleValue.valueOf(List.of(3, 4, 5)),
                                TupleValue.valueOf(List.of(6)))));
                        put("tuple_map", TupleValue.valueOf(List.of(
                                BaseValue.valueOf(Map.of("a", 0, "b", 1)),
                                BaseValue.valueOf(Map.of("a", 1, "b", 2)),
                                BaseValue.valueOf(Map.of("a", 2, "b", 3)))));
                        put("map", BaseValue.valueOf(Map.of("a", 0, "b", 1)));
                        put("map_object", BaseValue.valueOf(Map.of(
                                ObjectValue.valueOf("v", Map.of("b", "11", "a", 21)),
                                ObjectValue.valueOf("v", Map.of("b", "12", "a", 22)),
                                ObjectValue.valueOf("v", Map.of("b", "13", "a", 23)),
                                ObjectValue.valueOf("v", Map.of("b", "14", "a", 24)))));
                        put("map_list", BaseValue.valueOf(Map.of(
                                BaseValue.valueOf(List.of(1, 2)), BaseValue.valueOf(List.of(3, 4, 5)),
                                BaseValue.valueOf(List.of(6)), BaseValue.valueOf(List.of(7, 8, 9)))));
                        put("map_tuple", BaseValue.valueOf(Map.of(
                                TupleValue.valueOf(List.of(1, 2)), TupleValue.valueOf(List.of(3, 4, 5)),
                                TupleValue.valueOf(List.of(6)), TupleValue.valueOf(List.of(7, 8, 9)))));
                        put("map_map", BaseValue.valueOf(Map.of(
                                BaseValue.valueOf(Map.of("a", 0, "b", 1)),
                                BaseValue.valueOf(Map.of("a", 1, "b", 2)),
                                BaseValue.valueOf(Map.of("a", 2, "b", 3)),
                                BaseValue.valueOf(Map.of("a", 3, "b", 4)))));
                        put("object", ObjectValue.valueOf("o1", Map.of("a", 1, "b", "2")));
                        put("object_object", ObjectValue.valueOf("o2", Map.of("a", 1,
                                "b", ObjectValue.valueOf("v", Map.of("b", "11", "a", 21)))));
                        put("object_list", ObjectValue.valueOf("o3", Map.of("a", 1, "b", List.of(1, 2, 3))));
                        put("object_tuple",
                                ObjectValue.valueOf("o4", Map.of("a", 1, "b", TupleValue.valueOf(List.of(1, 2, 3)))));
                        put("object_map", ObjectValue.valueOf("o5", Map.of("a", 1, "b", Map.of("a", 0, "b", 1))));
                    }
                },
                new TreeMap<>() {
                    { // values with different types
                        put("string", BaseValue.valueOf(1));
                        put("bool", BaseValue.valueOf(1));
                        put("int8", BaseValue.valueOf(1));
                        put("int16", BaseValue.valueOf(1));
                        put("int32", BaseValue.valueOf("1"));
                        put("int64", BaseValue.valueOf(1));
                        put("float32", BaseValue.valueOf(1));
                        put("float64", BaseValue.valueOf(1));
                        put("bytes", BaseValue.valueOf(1));
                        put("unknown", BaseValue.valueOf(1));
                        put("list", BaseValue.valueOf(1));
                        put("list_object", BaseValue.valueOf(1));
                        put("list_list", BaseValue.valueOf(1));
                        put("list_tuple", BaseValue.valueOf(1));
                        put("list_map", BaseValue.valueOf(1));
                        put("tuple", BaseValue.valueOf(1));
                        put("tuple_object", BaseValue.valueOf(1));
                        put("tuple_list", BaseValue.valueOf(1));
                        put("tuple_tuple", BaseValue.valueOf(1));
                        put("tuple_map", BaseValue.valueOf(1));
                        put("map", BaseValue.valueOf(1));
                        put("map_object", BaseValue.valueOf(1));
                        put("map_list", BaseValue.valueOf(1));
                        put("map_tuple", BaseValue.valueOf(1));
                        put("map_map", BaseValue.valueOf(1));
                        put("object", BaseValue.valueOf(1));
                        put("object_object", BaseValue.valueOf(1));
                        put("object_list", BaseValue.valueOf(1));
                        put("object_tuple", BaseValue.valueOf(1));
                        put("object_map", BaseValue.valueOf(1));
                    }
                },
                new TreeMap<>() {
                    { // null values
                        put("string", null);
                        put("bool", null);
                        put("int8", null);
                        put("int16", null);
                        put("int32", null);
                        put("int64", null);
                        put("float32", null);
                        put("float64", null);
                        put("bytes", null);
                        put("unknown", null);
                        put("list", null);
                        put("list_object", null);
                        put("list_list", null);
                        put("list_tuple", null);
                        put("list_map", null);
                        put("tuple", null);
                        put("tuple_object", null);
                        put("tuple_list", null);
                        put("tuple_tuple", null);
                        put("tuple_map", null);
                        put("map", null);
                        put("map_object", null);
                        put("map_list", null);
                        put("map_tuple", null);
                        put("map_map", null);
                        put("object", null);
                        put("object_object", null);
                        put("object_list", null);
                        put("object_tuple", null);
                        put("object_map", null);
                    }
                },
                new TreeMap<>() {
                    { // replace scalar with list
                        put("string", BaseValue.valueOf(List.of(1)));
                        put("bool", BaseValue.valueOf(List.of(1)));
                        put("int8", BaseValue.valueOf(List.of(1)));
                        put("int16", BaseValue.valueOf(List.of(1)));
                        put("int32", BaseValue.valueOf(List.of(1)));
                        put("int64", BaseValue.valueOf(List.of(1)));
                        put("float32", BaseValue.valueOf(List.of(1)));
                        put("float64", BaseValue.valueOf(List.of(1)));
                        put("bytes", BaseValue.valueOf(List.of(1)));
                        put("unknown", BaseValue.valueOf(List.of(1)));
                    }
                },
                new TreeMap<>() {
                    { // mixed types
                        put("list", BaseValue.valueOf(List.of("1", 1, 1.0)));
                        put("list_object",
                                BaseValue.valueOf(List.of(
                                        ObjectValue.valueOf("v", Map.of("b", "11", "a", 21)),
                                        "1",
                                        ObjectValue.valueOf("v", Map.of("c", "11", "a", 21)))));
                        put("list_list", BaseValue.valueOf(List.of(
                                BaseValue.valueOf(List.of(1)),
                                "1",
                                BaseValue.valueOf(List.of("1", 1)),
                                TupleValue.valueOf(List.of(1)))));
                        put("list_tuple", BaseValue.valueOf(List.of(
                                TupleValue.valueOf(List.of(1)),
                                "1",
                                TupleValue.valueOf(List.of("1", 1)),
                                BaseValue.valueOf(List.of(1)))));
                        put("list_map", BaseValue.valueOf(List.of(
                                BaseValue.valueOf(Map.of("a", 0, "b", 1)),
                                "1",
                                BaseValue.valueOf(Map.of("a", 0, "b", 1, 0, "0")))));
                        put("tuple", TupleValue.valueOf(List.of("1", 1, 1.0)));
                        put("tuple_object", TupleValue.valueOf(List.of(
                                ObjectValue.valueOf("v", Map.of("b", "11", "a", 21)),
                                "1",
                                ObjectValue.valueOf("v", Map.of("c", "11", "a", 21)))));
                        put("tuple_list", TupleValue.valueOf(List.of(
                                BaseValue.valueOf(List.of(1)),
                                "1",
                                BaseValue.valueOf(List.of("1", 1)),
                                TupleValue.valueOf(List.of(1)))));
                        put("tuple_tuple", TupleValue.valueOf(List.of(
                                TupleValue.valueOf(List.of(1)),
                                "1",
                                TupleValue.valueOf(List.of("1", 1)),
                                BaseValue.valueOf(List.of(1)))));
                        put("tuple_map", TupleValue.valueOf(List.of(
                                BaseValue.valueOf(Map.of("a", 0, "b", 1)),
                                "1",
                                BaseValue.valueOf(Map.of("a", 0, "b", 1, 0, "0")))));
                        put("map", BaseValue.valueOf(Map.of("0", 1, 0, "1")));
                        put("map_object", BaseValue.valueOf(Map.of(
                                0, "1",
                                ObjectValue.valueOf("v", Map.of("b", "11", "a", 21)),
                                ObjectValue.valueOf("v", Map.of("b", "11", "a", 21)),
                                ObjectValue.valueOf("v", Map.of("c", "11", "a", 21)),
                                "1",
                                1,
                                ObjectValue.valueOf("v", Map.of("c", "11", "a", 21))
                        )));
                        put("map_list", BaseValue.valueOf(Map.of(
                                0, "1",
                                BaseValue.valueOf(List.of(1)), BaseValue.valueOf(List.of(1)),
                                BaseValue.valueOf(List.of(1, "1")), BaseValue.valueOf(List.of(1, "1")),
                                TupleValue.valueOf(List.of(1)), TupleValue.valueOf(List.of(1)))));
                        put("map_tuple", BaseValue.valueOf(Map.of(
                                0, "1",
                                BaseValue.valueOf(List.of(1)), BaseValue.valueOf(List.of(1)),
                                BaseValue.valueOf(List.of(1, "1")), BaseValue.valueOf(List.of(1, "1")),
                                TupleValue.valueOf(List.of(1)), TupleValue.valueOf(List.of(1)))));
                        put("map_map", BaseValue.valueOf(Map.of(
                                0, "1",
                                BaseValue.valueOf(Map.of("a", 0, "b", 1)),
                                BaseValue.valueOf(Map.of("a", 0, "b", 1)),
                                BaseValue.valueOf(Map.of("a", 0, "b", 1, 0, "0")),
                                BaseValue.valueOf(Map.of("a", 0, "b", 1, 0, "0")))));
                        put("object", ObjectValue.valueOf("v", Map.of("b", 1, "a", "0")));
                        put("object_object", ObjectValue.valueOf("v", Map.of("b", 1, "a", "0")));
                        put("object_list", ObjectValue.valueOf("v", Map.of("b", 1, "a", "0")));
                        put("object_tuple", ObjectValue.valueOf("v", Map.of("b", 1, "a", "0")));
                        put("object_map", ObjectValue.valueOf("v", Map.of("b", 1, "a", "0")));
                    }
                });
        SwWriter.writeWithBuilder(
                new SwParquetWriterBuilder(this.storageAccessService,
                        schema.getColumnSchemaList().stream()
                                .collect(Collectors.toMap(ColumnSchema::getName, Function.identity())),
                        schema.toJsonString(),
                        "meta",
                        "test",
                        parquetConfig),
                records.iterator());
        var conf = new Configuration();
        var reader = new SwParquetReaderBuilder(this.storageAccessService, "test").withConf(conf).build();
        assertThat(new TreeMap<>(reader.read()), is(records.get(0)));
        assertThat(conf.get(SwReadSupport.META_DATA_KEY), is("meta"));
        for (int i = 1; i < records.size(); ++i) {
            assertThat(new TreeMap<>(reader.read()), is(records.get(i)));
        }
        assertThat(reader.read(), nullValue());
        reader.close();
    }
}
