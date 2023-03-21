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

package ai.starwhale.mlops.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.datastore.ColumnDesc;
import ai.starwhale.mlops.api.protocol.datastore.ListTablesRequest;
import ai.starwhale.mlops.api.protocol.datastore.QueryTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.RecordDesc;
import ai.starwhale.mlops.api.protocol.datastore.RecordValueDesc;
import ai.starwhale.mlops.api.protocol.datastore.ScanTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.TableDesc;
import ai.starwhale.mlops.api.protocol.datastore.TableQueryFilterDesc;
import ai.starwhale.mlops.api.protocol.datastore.TableQueryOperandDesc;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.OrderByDesc;
import ai.starwhale.mlops.datastore.RecordList;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.datastore.WalManager;
import ai.starwhale.mlops.datastore.exporter.RecordsExporter;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import brave.internal.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DataStoreControllerTest {

    private DataStoreController controller;

    @BeforeEach
    public void setUp() {
        this.controller = new DataStoreController();
        var walManager = Mockito.mock(WalManager.class);
        given(walManager.readAll()).willReturn(Collections.emptyIterator());
        this.controller.setDataStore(
                new DataStore(new StorageAccessServiceMemory(),
                        65536,
                        65536,
                        3,
                        "",
                        "1h",
                        "1d",
                        "SNAPPY",
                        "1MB",
                        "1KB",
                        1000));
    }

    @Test
    public void testList() {
        var resp = this.controller.listTables(new ListTablesRequest());
        assertThat("empty", resp.getStatusCode().is2xxSuccessful(), is(true));
        assertThat("empty", Objects.requireNonNull(resp.getBody()).getData().getTables(), empty());

        this.controller.updateTable(new UpdateTableRequest() {
            {
                setTableName("t1");
                setTableSchemaDesc(
                        new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build())));
            }
        });
        this.controller.updateTable(new UpdateTableRequest() {
            {
                setTableName("test");
                setTableSchemaDesc(
                        new TableSchemaDesc("k", List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build())));
            }
        });
        resp = this.controller.listTables(new ListTablesRequest() {
            {
                setPrefix("te");
            }
        });
        assertThat("partial", resp.getStatusCode().is2xxSuccessful(), is(true));
        assertThat("partial", Objects.requireNonNull(resp.getBody()).getData().getTables(), is(List.of("test")));
    }

    @Test
    public void testUpdate() {
        this.controller.updateTable(new UpdateTableRequest() {
            {
                setTableName("t1");
                setTableSchemaDesc(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())));
                setRecords(List.of(new RecordDesc() {
                    {
                        setValues(List.of(new RecordValueDesc() {
                            {
                                setKey("k");
                                setValue("00000000");
                            }
                        }, new RecordValueDesc() {
                            {
                                setKey("a");
                                setValue("00000001");
                            }
                        }));
                    }
                }));
            }
        });
        this.controller.updateTable(new UpdateTableRequest() {
            {
                setTableName("t1");
                setTableSchemaDesc(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())));
                setRecords(List.of(new RecordDesc() {
                    {
                        setValues(List.of(new RecordValueDesc() {
                            {
                                setKey("k");
                                setValue("00000001");
                            }
                        }, new RecordValueDesc() {
                            {
                                setKey("a");
                                setValue("00000002");
                            }
                        }));
                    }
                }));
            }
        });
        this.controller.updateTable(new UpdateTableRequest() {
            {
                setTableName("t2");
                setTableSchemaDesc(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("x").type("INT32").build())));
                setRecords(List.of(new RecordDesc() {
                    {
                        setValues(List.of(new RecordValueDesc() {
                            {
                                setKey("k");
                                setValue("00000003");
                            }
                        }, new RecordValueDesc() {
                            {
                                setKey("x");
                                setValue("00000002");
                            }
                        }));
                    }
                }));
            }
        });
        this.controller.updateTable(new UpdateTableRequest() {
            {
                setTableName("t1");
                setTableSchemaDesc(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("a").type("INT32").build())));
                setRecords(List.of(new RecordDesc() {
                    {
                        setValues(List.of(new RecordValueDesc() {
                            {
                                setKey("k");
                                setValue("00000000");
                            }
                        }, new RecordValueDesc() {
                            {
                                setKey("-");
                                setValue("1");
                            }
                        }));
                    }
                }, new RecordDesc() {
                    {
                        setValues(List.of(new RecordValueDesc() {
                            {
                                setKey("k");
                                setValue("00000004");
                            }
                        }, new RecordValueDesc() {
                            {
                                setKey("-");
                                setValue("1");
                            }
                        }));
                    }
                }));
            }
        });
        this.controller.updateTable(new UpdateTableRequest() {
            {
                setTableName("t1");
                setTableSchemaDesc(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                ColumnSchemaDesc.builder().name("a").type("STRING").build())));
                setRecords(List.of(new RecordDesc() {
                    {
                        setValues(List.of(new RecordValueDesc() {
                            {
                                setKey("k");
                                setValue("00000000");
                            }
                        }, new RecordValueDesc() {
                            {
                                setKey("a");
                                setValue("1");
                            }
                        }));
                    }
                }));
            }
        });
        var req = new ScanTableRequest() {
            {
                setTables(List.of(new TableDesc() {
                    {
                        setTableName("t1");
                        setColumns(List.of(new ColumnDesc() {
                            {
                                setColumnName("k");
                            }
                        }, new ColumnDesc() {
                            {
                                setColumnName("a");
                                setAlias("b");
                            }
                        }));
                    }
                }));
            }
        };
        assertThrows(SwValidationException.class, () -> this.controller.scanTable(req));
        req.setEncodeWithType(true);
        var resp = this.controller.scanTable(req);
        assertThat("t1", resp.getStatusCode().is2xxSuccessful(), is(true));
        assertThat("t1",
                Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                nullValue());
        assertThat("t1",
                Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                is(List.of(Map.of("k", Map.of("type", "INT32", "value", "00000000"),
                                "b", Map.of("type", "STRING", "value", "1")),
                        Map.of("k", Map.of("type", "INT32", "value", "00000001"),
                                "b", Map.of("type", "INT32", "value", "00000002")))));
        resp = this.controller.scanTable(new ScanTableRequest() {
            {
                setTables(List.of(new TableDesc() {
                    {
                        setTableName("t2");
                        setColumns(List.of(new ColumnDesc() {
                            {
                                setColumnName("k");
                            }
                        }, new ColumnDesc() {
                            {
                                setColumnName("x");
                                setAlias("b");
                            }
                        }));
                    }
                }));
            }
        });
        assertThat("t2", resp.getStatusCode().is2xxSuccessful(), is(true));
        assertThat("t2",
                Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                        ColumnSchemaDesc.builder().name("b").type("INT32").build()));
        assertThat("t2",
                Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                is(List.of(Map.of("k", "00000003", "b", "00000002"))));
    }

    @Nested
    public class UpdateTest {

        private UpdateTableRequest req;

        @BeforeEach
        public void setUp() {
            this.req = new UpdateTableRequest() {
                {
                    setTableName("t1");
                    setTableSchemaDesc(new TableSchemaDesc("k",
                            List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                    ColumnSchemaDesc.builder().name("a").type("INT32").build())));
                    setRecords(List.of(new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000000");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000001");
                                }
                            }));
                        }
                    }));
                }
            };
        }

        @Test
        public void ensureRequest() {
            DataStoreControllerTest.this.controller.updateTable(req);
        }

        @Test
        public void testNullTableTable() {
            req.setTableName(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullSchema() {
            req.setTableSchemaDesc(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullKeyInSchema() {
            req.getTableSchemaDesc().setKeyColumn(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullNameInColumnSchema() {
            req.getTableSchemaDesc().getColumnSchemaList().get(0).setName(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullTypeInColumnSchema() {
            req.getTableSchemaDesc().getColumnSchemaList().get(0).setType(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testInvalidTypeInColumnSchema() {
            req.getTableSchemaDesc().getColumnSchemaList().get(0).setType("null");
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNoKeyColumnSchema() {
            req.getTableSchemaDesc().setColumnSchemaList(req.getTableSchemaDesc().getColumnSchemaList().subList(1, 1));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testDuplicateColumnSchema() {
            req.getTableSchemaDesc().setColumnSchemaList(
                    Lists.concat(req.getTableSchemaDesc().getColumnSchemaList(),
                            List.of(req.getTableSchemaDesc().getColumnSchemaList().get(1))));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullKeyInUpdates() {
            req.getRecords().get(0).getValues().get(0).setKey(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testInvalidKeyInUpdates() {
            req.getRecords().get(0).getValues().get(0).setKey("i");
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testInvalidValueInUpdates() {
            req.getRecords().get(0).getValues().get(1).setValue("i");
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }
    }

    @Nested
    public class QueryTest {

        private QueryTableRequest req;

        @BeforeEach
        public void setUp() {
            this.req = new QueryTableRequest() {
                {
                    setTableName("t1");
                    setColumns(List.of(new ColumnDesc() {
                        {
                            setColumnName("k");
                        }
                    }, new ColumnDesc() {
                        {
                            setColumnName("a");
                            setAlias("b");
                        }
                    }));
                    setFilter(new TableQueryFilterDesc() {
                        {
                            setOperator(TableQueryFilter.Operator.NOT.toString());
                            setOperands(List.of(new TableQueryOperandDesc() {
                                {
                                    setFilter(new TableQueryFilterDesc() {
                                        {
                                            setOperator(TableQueryFilter.Operator.AND.toString());
                                            setOperands(List.of(new TableQueryOperandDesc() {
                                                {
                                                    setFilter(new TableQueryFilterDesc() {
                                                        {
                                                            setOperator(TableQueryFilter.Operator.GREATER.toString());
                                                            setOperands(List.of(new TableQueryOperandDesc() {
                                                                {
                                                                    setColumnName("a");
                                                                }
                                                            }, new TableQueryOperandDesc() {
                                                                {
                                                                    setIntValue(1L);
                                                                }
                                                            }));
                                                        }
                                                    });
                                                }
                                            }, new TableQueryOperandDesc() {
                                                {
                                                    setFilter(new TableQueryFilterDesc() {
                                                        {
                                                            setOperator(TableQueryFilter.Operator.LESS.toString());
                                                            setOperands(List.of(new TableQueryOperandDesc() {
                                                                {
                                                                    setColumnName("a");
                                                                }
                                                            }, new TableQueryOperandDesc() {
                                                                {
                                                                    setIntValue(4L);
                                                                }
                                                            }));
                                                        }
                                                    });
                                                }
                                            }));
                                        }
                                    });
                                }
                            }));
                        }
                    });
                    setOrderBy(List.of(new OrderByDesc("a")));
                    setStart(1);
                    setLimit(2);
                }
            };
            DataStoreControllerTest.this.controller.updateTable(new UpdateTableRequest() {
                {
                    setTableName("t1");
                    setTableSchemaDesc(new TableSchemaDesc("k",
                            List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                    ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                                    ColumnSchemaDesc.builder().name("x").type("INT32").build())));
                    setRecords(List.of(new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000000");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000005");
                                }
                            }));
                        }
                    }, new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000001");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000004");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("x");
                                }
                            }));
                        }
                    }, new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000002");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000003");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("x");
                                    setValue("00000009");
                                }
                            }));
                        }
                    }, new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000003");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000002");
                                }
                            }));
                        }
                    }, new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000004");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000001");
                                }
                            }));
                        }
                    }));
                }
            });
        }

        @Test
        public void testQueryDefault() {
            var resp = DataStoreControllerTest.this.controller.queryTable(new QueryTableRequest() {
                {
                    setTableName("t1");
                }
            });
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("x").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "00000000", "a", "00000005"),
                            Map.of("k", "00000001", "a", "00000004"),
                            Map.of("k", "00000002", "a", "00000003", "x", "00000009"),
                            Map.of("k", "00000003", "a", "00000002"),
                            Map.of("k", "00000004", "a", "00000001"))));
        }

        @Test
        public void testQuery() {
            var resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "00000001", "b", "00000004"),
                            Map.of("k", "00000000", "b", "00000005"))));

            this.req.getOrderBy().get(0).setDescending(true);
            resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "00000001", "b", "00000004"),
                            Map.of("k", "00000004", "b", "00000001"))));

            this.req.setLimit(1);
            resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "00000001", "b", "00000004"))));

            this.req.setColumns(Lists.concat(this.req.getColumns(), List.of(new ColumnDesc() {
                {
                    setColumnName("x");
                }
            })));
            this.req.setKeepNone(true);
            resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("x").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(new HashMap<>() {
                        {
                            put("k", "00000001");
                            put("b", "00000004");
                            put("x", null);
                        }
                    })));

            this.req.setRawResult(true);
            resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("x").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(new HashMap<>() {
                        {
                            put("k", "1");
                            put("b", "4");
                            put("x", null);
                        }
                    })));

            this.req.setEncodeWithType(true);
            resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    nullValue());
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", Map.of("type", "INT32", "value", "1"),
                            "b", Map.of("type", "INT32", "value", "4"),
                            "x", new HashMap<>() {
                                {
                                    put("value", null);
                                }
                            }))));
        }

        @Test
        public void testNullTableName() {
            this.req.setTableName(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testTableNotExists() {
            this.req.setTableName("table not exists");
            this.req.setIgnoreNonExistingTable(false);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testTableNotExistsIgnore() {
            this.req.setTableName("table not exists");

            var resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes().isEmpty());
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords().isEmpty());
        }

        @Test
        public void testNullColumnName() {
            this.req.getColumns().get(0).setColumnName(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullColumnDesc() {
            this.req.setColumns(Lists.concat(this.req.getColumns(), new ArrayList<>() {
                {
                    add(null);
                }
            }));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidColumnName() {
            this.req.getColumns().get(0).setColumnName("i");
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullOrderBy() {
            this.req.setOrderBy(Lists.concat(this.req.getOrderBy(), new ArrayList<>() {
                {
                    add(null);
                }
            }));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidOrderBy() {
            this.req.setOrderBy(Lists.concat(this.req.getOrderBy(), new ArrayList<>() {
                {
                    add(new OrderByDesc("i"));
                }
            }));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullOperator() {
            this.req.getFilter().setOperator(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidOperator() {
            this.req.getFilter().setOperator("invalid");
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullOperands() {
            this.req.getFilter().setOperands(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testEqualNull() {
            this.req.setFilter(new TableQueryFilterDesc() {
                {
                    setOperator(TableQueryFilter.Operator.EQUAL.toString());
                    setOperands(List.of(new TableQueryOperandDesc() {
                        {
                            setColumnName("x");
                        }
                    }, new TableQueryOperandDesc()));
                }
            });
            var resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat(resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat(Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT32").build()));
            assertThat(Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(new HashMap<>() {
                        {
                            put("k", "00000003");
                            put("b", "00000002");
                        }
                    }, new HashMap<>() {
                        {
                            put("k", "00000001");
                            put("b", "00000004");
                        }
                    })));

        }

        @Test
        public void testNullValueInOperands() {
            this.req.getFilter().setOperands(new ArrayList<>() {
                {
                    add(null);
                }
            });
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidNumberOfOperands() {
            final var dummyFilter = new TableQueryOperandDesc() {
                {
                    setFilter(new TableQueryFilterDesc() {
                        {
                            setOperator(TableQueryFilter.Operator.EQUAL.toString());
                            setOperands(List.of(new TableQueryOperandDesc() {
                                {
                                    setColumnName("a");
                                }
                            }, new TableQueryOperandDesc() {
                                {
                                    setStringValue("00000001");
                                }
                            }));
                        }
                    });
                }
            };
            final var dummyColumn = new TableQueryOperandDesc() {
                {
                    setColumnName("a");
                }
            };
            final var dummyIntValue = new TableQueryOperandDesc() {
                {
                    setIntValue(1L);
                }
            };

            this.req.getFilter().setOperator(TableQueryFilter.Operator.NOT.toString());
            this.req.getFilter().setOperands(List.of());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperands(List.of(dummyFilter, dummyFilter));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperator(TableQueryFilter.Operator.AND.toString());
            this.req.getFilter().setOperands(List.of(dummyFilter));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperator(TableQueryFilter.Operator.OR.toString());
            this.req.getFilter().setOperands(List.of(dummyFilter));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperands(List.of(dummyColumn));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.EQUAL.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS_EQUAL.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER_EQUAL.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperands(List.of(dummyColumn, dummyIntValue, dummyIntValue));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.EQUAL.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS_EQUAL.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER_EQUAL.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidOperands() {
            final var dummyFilter = new TableQueryOperandDesc() {
                {
                    setFilter(new TableQueryFilterDesc() {
                        {
                            setOperator(TableQueryFilter.Operator.EQUAL.toString());
                            setOperands(List.of(new TableQueryOperandDesc() {
                                {
                                    setColumnName("a");
                                }
                            }, new TableQueryOperandDesc() {
                                {
                                    setStringValue("00000001");
                                }
                            }));
                        }
                    });
                }
            };
            final var dummyColumn = new TableQueryOperandDesc() {
                {
                    setColumnName("a");
                }
            };
            final var dummyIntValue = new TableQueryOperandDesc() {
                {
                    setIntValue(1L);
                }
            };

            this.req.getFilter().setOperands(List.of(dummyColumn));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.NOT.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperands(List.of(dummyColumn, dummyIntValue));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.AND.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.OR.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperands(List.of(dummyFilter, dummyIntValue));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.EQUAL.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS_EQUAL.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER_EQUAL.toString());
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

    }

    @Nested
    public class ScanTest {

        private ScanTableRequest req;

        @BeforeEach
        public void setUp() {
            this.req = new ScanTableRequest() {
                {
                    setTables(List.of(new TableDesc() {
                        {
                            setTableName("t1");
                            setColumns(List.of(new ColumnDesc() {
                                {
                                    setColumnName("k");
                                    setAlias("b");
                                }
                            }, new ColumnDesc() {
                                {
                                    setColumnName("a");
                                }
                            }));
                        }
                    }, new TableDesc() {
                        {
                            setTableName("t2");
                            setKeepNone(true);
                        }
                    }));
                    setStart("00000001");
                    setEnd("00000004");
                    setKeepNone(true);
                }
            };
            DataStoreControllerTest.this.controller.updateTable(new UpdateTableRequest() {
                {
                    setTableName("t1");
                    setTableSchemaDesc(new TableSchemaDesc("k",
                            List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                    ColumnSchemaDesc.builder().name("a").type("INT32").build())));
                    setRecords(List.of(new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000000");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000005");
                                }
                            }));
                        }
                    }, new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000001");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000004");
                                }
                            }));
                        }
                    }, new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000002");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000003");
                                }
                            }));
                        }
                    }, new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000003");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000002");
                                }
                            }));
                        }
                    }, new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000004");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000001");
                                }
                            }));
                        }
                    }));
                }
            });
            DataStoreControllerTest.this.controller.updateTable(new UpdateTableRequest() {
                {
                    setTableName("t2");
                    setTableSchemaDesc(new TableSchemaDesc("k",
                            List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                                    ColumnSchemaDesc.builder().name("a").type("INT32").build())));
                    setRecords(List.of(new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000001");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                    setValue("00000010");
                                }
                            }));
                        }
                    }, new RecordDesc() {
                        {
                            setValues(List.of(new RecordValueDesc() {
                                {
                                    setKey("k");
                                    setValue("00000002");
                                }
                            }, new RecordValueDesc() {
                                {
                                    setKey("a");
                                }
                            }));
                        }
                    }));
                }
            });
        }

        @Test
        public void testScanDefault() {
            var resp = DataStoreControllerTest.this.controller.scanTable(new ScanTableRequest() {
                {
                    setTables(List.of(new TableDesc() {
                        {
                            setTableName("t1");
                        }
                    }));
                }
            });
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("a").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "00000000", "a", "00000005"),
                            Map.of("k", "00000001", "a", "00000004"),
                            Map.of("k", "00000002", "a", "00000003"),
                            Map.of("k", "00000003", "a", "00000002"),
                            Map.of("k", "00000004", "a", "00000001"))));
            assertThat("test", Objects.requireNonNull(resp.getBody()).getData().getLastKey(), is("00000004"));
        }

        @Test
        public void testScan() {
            var resp = DataStoreControllerTest.this.controller.scanTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "00000001", "b", "00000001", "a", "00000010"),
                            new HashMap<>() {
                                {
                                    put("k", "00000002");
                                    put("b", "00000002");
                                    put("a", null);
                                }
                            },
                            Map.of("b", "00000003", "a", "00000002"))));

            this.req.setLimit(1);
            resp = DataStoreControllerTest.this.controller.scanTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "00000001", "b", "00000001", "a", "00000010"))));
            assertThat("test", Objects.requireNonNull(resp.getBody()).getData().getLastKey(), is("00000001"));

            this.req.setRawResult(true);
            resp = DataStoreControllerTest.this.controller.scanTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("b").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "1", "b", "1", "a", "16"))));
            assertThat("test", Objects.requireNonNull(resp.getBody()).getData().getLastKey(), is("00000001"));

            this.req.getTables().get(0).setColumnPrefix("x");
            resp = DataStoreControllerTest.this.controller.scanTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    containsInAnyOrder(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("xb").type("INT32").build(),
                            ColumnSchemaDesc.builder().name("xa").type("INT32").build()));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("xa", "4", "xb", "1", "k", "1", "a", "16"))));
            assertThat("test", Objects.requireNonNull(resp.getBody()).getData().getLastKey(), is("00000001"));
        }

        @Test
        public void testNullTableDesc() {
            this.req.setTables(Lists.concat(this.req.getTables(), new ArrayList<>() {
                {
                    add(null);
                }
            }));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testNullTableName() {
            this.req.getTables().get(0).setTableName(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testInvalidTableName() {
            this.req.getTables().get(0).setTableName("invalid");
            this.req.setIgnoreNonExistingTable(false);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testTableNotExistsIgnore() {
            this.req.getTables().get(0).setTableName("invalid");

            var resp = DataStoreControllerTest.this.controller.scanTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
        }

        @Test
        public void testNullColumn() {
            this.req.getTables().get(0).setColumns(
                    Lists.concat(this.req.getTables().get(0).getColumns(),
                            new ArrayList<>() {
                                {
                                    add(null);
                                }
                            }));
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testNullColumnName() {
            this.req.getTables().get(0).getColumns().get(0).setColumnName(null);
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testInvalidColumnName() {
            this.req.getTables().get(0).getColumns().get(0).setColumnName("i");
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testInvalidStart() {
            this.req.setStart("i");
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testInvalidEnd() {
            this.req.setEnd("i");
            assertThrows(SwValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }
    }

    @Test
    public void testExport() throws IOException {
        DataStoreController controller = new DataStoreController();
        RecordsExporter exporter = mock(RecordsExporter.class);
        controller.setRecordsExporter(exporter);
        DataStore dataStore = mock(DataStore.class);
        controller.setDataStore(dataStore);
        byte[] content = "hello".getBytes();
        when(exporter.asBytes(any())).thenReturn(content);
        when(dataStore.query(any())).thenReturn(new RecordList(null, null, null, null));
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);
        controller.queryAndExport(new QueryTableRequest() {
            {
                setTableName("t1");
            }
        }, response);
        verify(outputStream).write(content);
    }
}