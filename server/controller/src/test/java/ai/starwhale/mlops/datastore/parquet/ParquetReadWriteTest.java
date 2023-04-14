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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.datastore.ColumnSchema;
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ParquetConfig;
import ai.starwhale.mlops.datastore.ParquetConfig.CompressionCodec;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.ObjectValue;
import ai.starwhale.mlops.datastore.type.TupleValue;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        var schema = new TableSchema(new TableSchemaDesc("key", List.of(
                ColumnSchemaDesc.builder().name("key").type("STRING").build(),
                ColumnSchemaDesc.builder().name("a").type("BOOL").build(),
                ColumnSchemaDesc.builder().name("b").type("INT8").build(),
                ColumnSchemaDesc.builder().name("c").type("INT16").build(),
                ColumnSchemaDesc.builder().name("d").type("INT32").build(),
                ColumnSchemaDesc.builder().name("e").type("INT64").build(),
                ColumnSchemaDesc.builder().name("f").type("FLOAT32").build(),
                ColumnSchemaDesc.builder().name("g").type("FLOAT64").build(),
                ColumnSchemaDesc.builder().name("h").type("BYTES").build(),
                ColumnSchemaDesc.builder().name("i").type("UNKNOWN").build(),
                ColumnSchemaDesc.builder().name("j")
                        .type("LIST")
                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                        .build(),
                ColumnSchemaDesc.builder().name("jj")
                        .type("MAP")
                        .keyType(ColumnSchemaDesc.builder().type("STRING").build())
                        .valueType(ColumnSchemaDesc.builder().type("INT32").build())
                        .build(),
                ColumnSchemaDesc.builder().name("k")
                        .type("OBJECT")
                        .pythonType("placeholder")
                        .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("b").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("aa").type("STRING").build()))
                        .build(),
                ColumnSchemaDesc.builder().name("l")
                        .type("LIST")
                        .elementType(ColumnSchemaDesc.builder()
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder()
                                        .type("OBJECT")
                                        .pythonType("v")
                                        .attributes(List.of(
                                                ColumnSchemaDesc.builder().name("a")
                                                        .type("OBJECT")
                                                        .pythonType("t")
                                                        .attributes(List.of(
                                                                ColumnSchemaDesc.builder().name("a")
                                                                        .type("INT32")
                                                                        .build(),
                                                                ColumnSchemaDesc.builder().name("b")
                                                                        .type("INT32")
                                                                        .build()))
                                                        .build(),
                                                ColumnSchemaDesc.builder().name("b")
                                                        .type("LIST")
                                                        .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build(),
                ColumnSchemaDesc.builder().name("m")
                        .type("MAP")
                        .keyType(ColumnSchemaDesc.builder().type("TUPLE")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())
                        .valueType(ColumnSchemaDesc.builder()
                                .type("OBJECT")
                                .pythonType("placeholder")
                                .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                        ColumnSchemaDesc.builder().name("b").type("INT32").build()))
                                .build())
                        .build())));
        var parquetConfig = new ParquetConfig();
        parquetConfig.setCompressionCodec(CompressionCodec.SNAPPY);
        parquetConfig.setRowGroupSize(1024 * 1024);
        parquetConfig.setPageSize(4096);
        parquetConfig.setPageRowCountLimit(1000);
        List<Map<String, BaseValue>> records = List.of(
                new HashMap<>() {
                    {
                        put("key", BaseValue.valueOf("0"));
                        put("a", BaseValue.valueOf(true));
                        put("b", BaseValue.valueOf((byte) 2));
                        put("c", BaseValue.valueOf((short) 3));
                        put("d", BaseValue.valueOf(4));
                        put("e", BaseValue.valueOf(5L));
                        put("f", BaseValue.valueOf(1.1f));
                        put("g", BaseValue.valueOf(1.2));
                        put("h", BaseValue.valueOf(ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8))));
                        put("i", null);
                        put("j", BaseValue.valueOf(List.of(10)));
                        put("jj", BaseValue.valueOf(Map.of("a", 0, "b", 1)));
                        put("k", ObjectValue.valueOf("t", Map.of("b", 11, "a", 12, "aa", "13")));
                        put("l", BaseValue.valueOf(List.of(List.of(
                                ObjectValue.valueOf("v",
                                        Map.of("a", ObjectValue.valueOf("t", Map.of("b", 3, "a", 4)),
                                                "b", List.of(100)))))));
                        put("m", BaseValue.valueOf(
                                Map.of(TupleValue.valueOf(List.of(1, 2, 3)),
                                        ObjectValue.valueOf("t", Map.of("a", 1, "b", 2)))));
                    }
                },
                Map.of("key", BaseValue.valueOf("1"), "a", BaseValue.valueOf(false)),
                Map.of("key", BaseValue.valueOf("2"), "j", BaseValue.valueOf(List.of(1, 2))),
                Map.of("key", BaseValue.valueOf("3"), "k", ObjectValue.valueOf("t", Map.of("a", 1))),
                Map.of("key", BaseValue.valueOf("4")),
                Map.of("key", BaseValue.valueOf("5"),
                        "j", BaseValue.valueOf(new ArrayList<>() {
                            {
                                add(3);
                                add(null);
                                add(4);
                            }
                        }),
                        "jj", BaseValue.valueOf(new HashMap<>() {
                            {
                                put("a", 0);
                                put("b", 1);
                                put("x", null);
                                put(null, 3);
                                put(null, null);
                            }
                        })),
                Map.of("key", BaseValue.valueOf("6"),
                        "j", BaseValue.valueOf(List.of()),
                        "jj", BaseValue.valueOf(Map.of()),
                        "k", ObjectValue.valueOf("t", Map.of())),
                Map.of("key", BaseValue.valueOf(7),
                        "j", BaseValue.valueOf(8),
                        "jj", BaseValue.valueOf(new HashMap<>() {
                            {
                                put("a", "0");
                                put(0, 1);
                                put(null, "1");
                                put(3, null);
                                put("x", 4);
                            }
                        }),
                        "k", ObjectValue.valueOf("tt", Map.of("a", 0)),
                        "m", BaseValue.valueOf(
                                Map.of(TupleValue.valueOf(List.of(1, "2", 3)),
                                        ObjectValue.valueOf("xx", Map.of("c", 1, "d", 2L)))),
                        "extra_field", BaseValue.valueOf(0)),
                Map.of("key", BaseValue.valueOf(8L),
                        "j", BaseValue.valueOf(new ArrayList<>() {
                            {
                                add("0");
                                add(null);
                                add(4);
                                add(Map.of());
                            }
                        }),
                        "k", ObjectValue.valueOf("tt", Map.of("a", "0")),
                        "l", BaseValue.valueOf(List.of(List.of(Map.of("a", Map.of("a", "b")))))));
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
        assertThat(new HashMap<>(reader.read()), is(records.get(0)));
        assertThat(conf.get(SwReadSupport.META_DATA_KEY), is("meta"));
        assertThat(new HashMap<>(reader.read()), is(records.get(1)));
        assertThat(new HashMap<>(reader.read()), is(records.get(2)));
        assertThat(new HashMap<>(reader.read()), is(records.get(3)));
        assertThat(new HashMap<>(reader.read()), is(records.get(4)));
        assertThat(new HashMap<>(reader.read()), is(records.get(5)));
        assertThat(new HashMap<>(reader.read()), is(records.get(6)));
        assertThat(new HashMap<>(reader.read()), is(records.get(7)));
        assertThat(new HashMap<>(reader.read()), is(records.get(8)));
        assertThat(reader.read(), nullValue());
        reader.close();
    }

    @Test
    public void testReadTheErrorWrite() {
        var schema = new TableSchema(new TableSchemaDesc("id", List.of(
                ColumnSchemaDesc.builder().name("id").type("INT64").build(),
                ColumnSchemaDesc.builder()
                        .name("data/img")
                        .type("OBJECT")
                        .pythonType("starwhale.core.dataset.type.GrayscaleImage")
                        .attributes(List.of(
                                ColumnSchemaDesc.builder()
                                        .name("link")
                                        .type("OBJECT")
                                        .pythonType("starwhale.core.dataset.type.Link")
                                        .attributes(List.of(
                                                ColumnSchemaDesc.builder().name("uri").type("STRING").build(),
                                                ColumnSchemaDesc.builder()
                                                        .name("extra_info")
                                                        .type("MAP")
                                                        .keyType(ColumnSchemaDesc.builder().type("STRING").build())
                                                        .valueType(ColumnSchemaDesc.builder().type("INT64").build())
                                                        .build(),
                                                ColumnSchemaDesc.builder().name("owner").type("UNKNOWN").build()
                                        ))
                                        .build(),
                                ColumnSchemaDesc.builder().name("_BaseArtifact__cache_bytes").type("BYTES").build(),
                                ColumnSchemaDesc.builder().name("owner").type("UNKNOWN").build()
                        )).build())));

        List<Map<String, BaseValue>> records = List.of(
                new HashMap<>() {
                    {
                        put("id", BaseValue.valueOf("0000000000000000"));
                        put("data/img", ObjectValue.valueOf("starwhale.core.dataset.type.GrayscaleImage",
                                new HashMap<>() {
                                    {
                                        put("link", ObjectValue.valueOf("starwhale.core.dataset.type.Link",
                                                new HashMap<>() {
                                                    {
                                                        put("extra_info", Map.of(
                                                                "bin_offset", "0000000000000000",
                                                                "bin_size",
                                                                "00000000000003e0"));
                                                        put("uri", "11111111111111111");
                                                        put("owner", null);
                                                    }
                                                }));
                                        put("_BaseArtifact__cache_bytes",
                                                ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)));
                                        put("owner", null);
                                    }
                                }));
                    }
                }
        );

        var parquetConfig = new ParquetConfig();
        parquetConfig.setCompressionCodec(CompressionCodec.SNAPPY);
        parquetConfig.setRowGroupSize(1024 * 1024);
        parquetConfig.setPageSize(4096);
        parquetConfig.setPageRowCountLimit(1000);

        assertThrows(UnsupportedOperationException.class, () -> SwWriter.writeWithBuilder(
                new SwParquetWriterBuilder(
                        this.storageAccessService,
                        schema.getColumnSchemaList().stream()
                                .collect(Collectors.toMap(ColumnSchema::getName, Function.identity())),
                        schema.toJsonString(),
                        "meta",
                        "test",
                        parquetConfig),
                records.stream().iterator()));

        assertThrows(SwValidationException.class, () -> {
            try (var reader = new SwParquetReaderBuilder(
                    this.storageAccessService, "test").withConf(new Configuration()).build()) {
                reader.read();
            }
        });
    }

}
