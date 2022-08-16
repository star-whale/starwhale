package ai.starwhale.mlops.datastore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class DataStoreTest {

    private DataStore dataStore;

    @BeforeEach
    public void setUp() {
        this.dataStore = new DataStore();
    }

    @Test
    public void testUpdate() {
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "STRING"),
                                new ColumnSchemaDesc("a", "INT32"))),
                List.of(Map.of("k", "0", "a", "1")));
        this.dataStore.update("t1",
                null,
                List.of(Map.of("k", "1", "a", "2")));
        this.dataStore.update("t2",
                new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "STRING"),
                                new ColumnSchemaDesc("x", "INT32"))),
                List.of(Map.of("k", "3", "x", "2")));
        this.dataStore.update("t1",
                null,
                List.of(Map.of("k", "0", "-", "1"), Map.of("k", "4", "-", "1")));
        assertThat("t1",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t1")
                                        .columns(Map.of("k", "k", "a", "a"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(Map.of("k", "1", "a", "2"))));
        assertThat("t2",
                this.dataStore.scan(DataStoreScanRequest.builder()
                                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                        .tableName("t2")
                                        .columns(Map.of("k", "k", "x", "x"))
                                        .build()))
                                .build())
                        .getRecords(),
                is(List.of(Map.of("k", "3", "x", "2"))));
    }

    @Test
    public void testQuery() {
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "STRING"),
                                new ColumnSchemaDesc("a", "INT32"))),
                List.of(Map.of("k", "0", "a", "5"),
                        Map.of("k", "1", "a", "4"),
                        Map.of("k", "2", "a", "3"),
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1")));
        var recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName("t1")
                .columns(Map.of("a", "a"))
                .filter(TableQueryFilter.builder()
                        .operator(TableQueryFilter.Operator.GREATER)
                        .operands(List.of(new TableQueryFilter.Column("a"), 1))
                        .build())
                .orderBy(List.of(new OrderByDesc("a")))
                .start(1)
                .limit(2)
                .build());
        assertThat("test", recordList.getColumnTypeMap(), is(Map.of("a", ColumnType.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("a", "3"),
                        Map.of("a", "4"))));
    }

    @Test
    public void testScanOneTable() {
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "STRING"),
                                new ColumnSchemaDesc("a", "INT32"))),
                List.of(Map.of("k", "0", "a", "5"),
                        Map.of("k", "1", "a", "4"),
                        new HashMap<>() {{
                            put("k", "2");
                            put("a", null);
                        }},
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1")));
        var recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t1")
                        .columns(Map.of("a", "a"))
                        .keepNone(true)
                        .build()))
                .start("1")
                .startInclusive(true)
                .end("3")
                .endInclusive(true)
                .keepNone(true)
                .build());
        assertThat("test", recordList.getColumnTypeMap(), is(Map.of("a", ColumnType.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("a", "4"),
                        new HashMap<>() {{
                            put("a", null);
                        }},
                        Map.of("a", "2"))));
        assertThat("test", recordList.getLastKey(), is("3"));


        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                        .tableName("t1")
                        .columns(Map.of("a", "a"))
                        .keepNone(true)
                        .build()))
                .start("1")
                .startInclusive(true)
                .end("3")
                .endInclusive(true)
                .limit(2)
                .build());
        assertThat("test", recordList.getColumnTypeMap(), is(Map.of("a", ColumnType.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("a", "4"), Map.of())));
        assertThat("test", recordList.getLastKey(), is("2"));
    }

    @Test
    public void testScanMultipleTables() {
        this.dataStore.update("t1",
                new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "STRING"),
                                new ColumnSchemaDesc("a", "INT32"))),
                List.of(Map.of("k", "0", "a", "5"),
                        Map.of("k", "1", "a", "4"),
                        Map.of("k", "2", "a", "3"),
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1")));
        this.dataStore.update("t2",
                new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "STRING"),
                                new ColumnSchemaDesc("b", "INT32"))),
                List.of(Map.of("k", "0", "b", "15"),
                        Map.of("k", "2", "b", "13"),
                        Map.of("k", "4", "b", "11")));
        this.dataStore.update("t3",
                new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "STRING"),
                                new ColumnSchemaDesc("a", "INT32"))),
                List.of(Map.of("k", "2")));
        var recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t1")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t2")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t3")
                                .keepNone(true)
                                .build()))
                .keepNone(true)
                .build());
        assertThat("test",
                recordList.getColumnTypeMap(),
                is(Map.of("k",
                        ColumnType.STRING,
                        "a",
                        ColumnType.INT32,
                        "b",
                        ColumnType.INT32)));
        assertThat("test",
                recordList.getRecords(),
                is(List.of(Map.of("k", "0", "a", "5", "b", "15"),
                        Map.of("k", "1", "a", "4"),
                        new HashMap<>() {{
                            put("k", "2");
                            put("a", null);
                            put("b", "13");
                        }},
                        Map.of("k", "3", "a", "2"),
                        Map.of("k", "4", "a", "1", "b", "11"))));
        assertThat("test", recordList.getLastKey(), is("4"));

        recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                .tables(List.of(DataStoreScanRequest.TableInfo.builder()
                                .tableName("t1")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t2")
                                .keepNone(true)
                                .build(),
                        DataStoreScanRequest.TableInfo.builder()
                                .tableName("t3")
                                .keepNone(true)
                                .build()))
                .start("7")
                .build());
        assertThat("empty", recordList.getColumnTypeMap(), nullValue());
        assertThat("empty", recordList.getRecords(), nullValue());
        assertThat("empty", recordList.getLastKey(), nullValue());
    }

}
