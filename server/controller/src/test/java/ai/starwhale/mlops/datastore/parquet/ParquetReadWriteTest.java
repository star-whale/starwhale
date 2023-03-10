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

import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ParquetConfig;
import ai.starwhale.mlops.datastore.ParquetConfig.CompressionCodec;
import ai.starwhale.mlops.datastore.TableSchema;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                        .pythonType("t")
                        .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("b").type("INT32").build()))
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
                        .build())));
        var parquetConfig = new ParquetConfig();
        parquetConfig.setCompressionCodec(CompressionCodec.SNAPPY);
        parquetConfig.setRowGroupSize(1024 * 1024);
        parquetConfig.setPageSize(4096);
        parquetConfig.setPageRowCountLimit(1000);
        var writer = new SwParquetWriterBuilder(this.storageAccessService,
                schema.getColumnTypeMapping(),
                schema.toJsonString(),
                "meta",
                "test",
                parquetConfig).build();
        List<Map<String, Object>> records = List.of(
                new HashMap<>() {
                    {
                        put("key", "0");
                        put("a", true);
                        put("b", (byte) 2);
                        put("c", (short) 3);
                        put("d", 4);
                        put("e", 5L);
                        put("f", 1.1f);
                        put("g", 1.2);
                        put("h", ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)));
                        put("i", null);
                        put("j", List.of(10));
                        put("jj", Map.of("a", 0, "b", 1));
                        put("k", Map.of("b", 11, "a", 12));
                        put("l", List.of(List.of(Map.of("a", Map.of("b", 3, "a", 4), "b", List.of(100)))));
                    }
                },
                Map.of("key", "1", "a", false),
                Map.of("key", "2", "j", List.of(1, 2)),
                Map.of("key", "3", "k", Map.of("a", 1)),
                Map.of("key", "4"),
                Map.of("key", "5", "j", new ArrayList<>() {
                    {
                        add(3);
                        add(null);
                        add(4);
                    }
                }, "jj", new HashMap<>() {
                    {
                        put("a", 0);
                        put("b", 1);
                        put("x", null);
                    }
                }),
                Map.of("key", "6", "j", List.of(), "jj", Map.of(), "k", Map.of()));
        for (var record : records) {
            writer.write(record);
        }
        writer.close();
        var conf = new Configuration();
        var reader = new SwParquetReaderBuilder(this.storageAccessService, "test").withConf(conf).build();
        assertThat(reader.read(), is(records.get(0)));
        assertThat(conf.get(SwReadSupport.META_DATA_KEY), is("meta"));
        assertThat(reader.read(), is(records.get(1)));
        assertThat(reader.read(), is(records.get(2)));
        assertThat(reader.read(), is(records.get(3)));
        assertThat(reader.read(), is(records.get(4)));
        assertThat(reader.read(), is(records.get(5)));
        assertThat(reader.read(), is(records.get(6)));
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

        List<Map<String, Object>> records = List.of(
            new HashMap<>() {
                {
                    put("id", "0000000000000000");
                    put("data/img", new HashMap<String, Object>() {
                        {
                            put("link", new HashMap<String, Object>() {
                                {
                                    put("extra_info", Map.of(
                                            "bin_offset", "0000000000000000",
                                            "bin_size", "00000000000003e0")
                                    );
                                    put("uri", "11111111111111111");
                                    put("owner", null);
                                }
                            });
                            put("_BaseArtifact__cache_bytes", ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)));
                            put("owner", null);
                        }
                    });
                }
            }
        );

        var parquetConfig = new ParquetConfig();
        parquetConfig.setCompressionCodec(CompressionCodec.SNAPPY);
        parquetConfig.setRowGroupSize(1024 * 1024);
        parquetConfig.setPageSize(4096);
        parquetConfig.setPageRowCountLimit(1000);

        assertThrows(UnsupportedOperationException.class, () -> {
            var builder = new SwParquetWriterBuilder(
                    this.storageAccessService,
                    schema.getColumnTypeMapping(),
                    schema.toJsonString(),
                    "meta",
                    "test",
                    parquetConfig);
            var writer = builder.build();
            try {
                for (var record : records) {
                    writer.write(record);
                }
                builder.success();
            } catch (Throwable e) {
                builder.error();
                throw e;
            } finally {
                writer.close();
            }
        });

        assertThrows(SwProcessException.class, () -> {
            try (var reader = new SwParquetReaderBuilder(
                    this.storageAccessService, "test").withConf(new Configuration()).build()) {
                reader.read();
            }
        });
    }

}
