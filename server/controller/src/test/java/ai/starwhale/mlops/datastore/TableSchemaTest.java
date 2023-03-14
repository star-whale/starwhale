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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TableSchemaTest {

    private TableSchemaDesc desc;
    private TableSchema schema;
    private final ColumnSchemaDesc listSchemaDesc = ColumnSchemaDesc.builder()
            .name("list")
            .type("LIST")
            .elementType(ColumnSchemaDesc.builder().name("element").type("STRING").build())
            .build();
    private final ColumnSchemaDesc mapSchemaDesc = ColumnSchemaDesc.builder()
            .name("map")
            .type("MAP")
            .keyType(ColumnSchemaDesc.builder().name("key").type("STRING").build())
            .valueType(ColumnSchemaDesc.builder().name("value").type("INT32").build())
            .build();
    private final ColumnSchemaDesc objectSchemaDesc = ColumnSchemaDesc.builder()
            .name("obj")
            .type("OBJECT")
            .pythonType("t")
            .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                    ColumnSchemaDesc.builder().name("b").type("INT64").build()))
            .build();

    @BeforeEach
    public void setUp() {
        this.desc = new TableSchemaDesc("k",
                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                        ColumnSchemaDesc.builder().name("a").type("INT32").build(),
                        this.listSchemaDesc,
                        this.mapSchemaDesc,
                        this.objectSchemaDesc));
        this.schema = new TableSchema(this.desc);
    }

    @Test
    public void testConstructor() {
        assertThat(this.schema.getKeyColumn(), is("k"));
        assertThat(this.schema.getColumnSchemaByName("list").toColumnSchemaDesc(), is(this.listSchemaDesc));
        assertThat(this.schema.getColumnSchemaByName("map").toColumnSchemaDesc(), is(this.mapSchemaDesc));
        assertThat(this.schema.getColumnSchemaByName("obj").toColumnSchemaDesc(), is(this.objectSchemaDesc));
        new TableSchema(new TableSchemaDesc(
                "k",
                List.of(ColumnSchemaDesc.builder().name("k").type("STRING").build(),
                        ColumnSchemaDesc.builder().name("123").type("INT8").build(),
                        ColumnSchemaDesc.builder().name("a/b-c/d:e_f").type("INT8").build())));
    }

    @Test
    public void testGetDiffAndUpdateEmpty() {
        var schema = new TableSchema();
        var diff = schema.getDiff(this.desc);
        var expectedDiff = this.schema.toWal();
        var listDiff = expectedDiff.getColumnsBuilder(2);
        listDiff.setElementType(listDiff.getElementTypeBuilder().setColumnName(""));
        var mapDiff = expectedDiff.getColumnsBuilder(3);
        mapDiff.setKeyType(mapDiff.getKeyTypeBuilder().setColumnName(""))
                .setValueType(mapDiff.getValueTypeBuilder().setColumnName(""));
        assertThat(diff.build(), is(expectedDiff.setColumns(2, listDiff).setColumns(3, mapDiff).build()));
        schema.update(this.schema.toWal().build());
        assertThat(schema.toWal().build(), is(this.schema.toWal().build()));
    }

    @Test
    public void testGetDiffAndUpdateNoDiff() {
        var diff = this.schema.getDiff(this.desc);
        assertThat(diff, nullValue());
        var wal = this.schema.toWal().build();
        this.schema.update(new TableSchema().getDiff(this.desc).build());
        assertThat(this.schema.toWal().build(), is(wal));
    }

    @Test
    public void testGetDiffAndUpdateColumnType() {
        var diff = this.schema.getDiff(new TableSchemaDesc("k",
                List.of(ColumnSchemaDesc.builder().name("k").type("INT32").build(),
                        ColumnSchemaDesc.builder().name("list").type("STRING").build()))).build();
        var newK = Wal.ColumnSchema.newBuilder()
                .setColumnName("k")
                .setColumnType("INT32")
                .setColumnIndex(this.schema.getColumnSchemaByName("k").getIndex());
        var newList = Wal.ColumnSchema.newBuilder()
                .setColumnName("list")
                .setColumnType("STRING")
                .setColumnIndex(this.schema.getColumnSchemaByName("list").getIndex());
        assertThat(diff, is(Wal.TableSchema.newBuilder()
                .addColumns(newK)
                .addColumns(newList)
                .build()));
        var walMap = this.schema.getColumnSchemaList().stream()
                .collect(Collectors.toMap(ColumnSchema::getName, ColumnSchema::toWal));
        walMap.put("k", newK);
        walMap.put("list", newList);
        this.schema.update(diff);
        walMap.forEach((k, v) -> assertThat(this.schema.getColumnSchemaByName(k).toWal().build(), is(v.build())));
    }

    @Test
    public void testGetDiffAndUpdateNewColumn() {
        var diff = this.schema.getDiff(
                        new TableSchemaDesc("k",
                                List.of(ColumnSchemaDesc.builder().name("b").type("INT16").build(),
                                        ColumnSchemaDesc.builder()
                                                .name("c")
                                                .type("LIST")
                                                .elementType(ColumnSchemaDesc.builder()
                                                        .type("OBJECT")
                                                        .pythonType("x")
                                                        .attributes(List.of(
                                                                ColumnSchemaDesc.builder()
                                                                        .name("a")
                                                                        .type("INT32")
                                                                        .build(),
                                                                ColumnSchemaDesc.builder()
                                                                        .name("b")
                                                                        .type("STRING")
                                                                        .build()
                                                        ))
                                                        .build())
                                                .build())))
                .build();
        var newB = Wal.ColumnSchema.newBuilder()
                .setColumnName("b")
                .setColumnType("INT16")
                .setColumnIndex(this.schema.getColumnSchemaList().size());
        var newC = Wal.ColumnSchema.newBuilder()
                .setColumnName("c")
                .setColumnType("LIST")
                .setElementType(Wal.ColumnSchema.newBuilder()
                        .setColumnType("OBJECT")
                        .setPythonType("x")
                        .addAttributes(Wal.ColumnSchema.newBuilder()
                                .setColumnName("a")
                                .setColumnType("INT32")
                                .build())
                        .addAttributes(Wal.ColumnSchema.newBuilder()
                                .setColumnName("b")
                                .setColumnType("STRING")
                                .build())
                        .build())
                .setColumnIndex(this.schema.getColumnSchemaList().size() + 1);
        assertThat(diff, is(Wal.TableSchema.newBuilder()
                .addColumns(newB)
                .addColumns(newC)
                .build()));
        var walMap = this.schema.getColumnSchemaList().stream()
                .collect(Collectors.toMap(ColumnSchema::getName, ColumnSchema::toWal));
        this.schema.update(diff);
        assertThat(this.schema.getColumnSchemaByName("b").toWal().build(), is(newB.build()));
        newC.setElementType(newC.getElementTypeBuilder().setColumnName("element"));
        assertThat(this.schema.getColumnSchemaByName("c").toWal().build(), is(newC.build()));
        walMap.forEach((k, v) -> assertThat(this.schema.getColumnSchemaByName(k).toWal().build(), is(v.build())));
    }

    @Test
    public void testGetDiffAndUpdateListElement() {
        var diff = this.schema.getDiff(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder()
                                .name("list")
                                .type("LIST")
                                .elementType(ColumnSchemaDesc.builder().type("INT32").build())
                                .build())))
                .build();
        var newList = Wal.ColumnSchema.newBuilder()
                .setColumnName("list")
                .setColumnType("LIST")
                .setColumnIndex(this.schema.getColumnSchemaByName("list").getIndex())
                .setElementType(Wal.ColumnSchema.newBuilder()
                        .setColumnType("INT32")
                        .build());
        assertThat(diff, is(Wal.TableSchema.newBuilder().addColumns(newList).build()));
        var walMap = this.schema.getColumnSchemaList().stream()
                .collect(Collectors.toMap(ColumnSchema::getName, ColumnSchema::toWal));
        newList.setElementType(newList.getElementTypeBuilder().setColumnName("element"));
        walMap.put("list", newList);
        this.schema.update(diff);
        walMap.forEach((k, v) -> assertThat(this.schema.getColumnSchemaByName(k).toWal().build(), is(v.build())));
    }

    @Test
    public void testGetDiffAndUpdateMapKey() {
        var diff = this.schema.getDiff(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder()
                                .name("map")
                                .type("MAP")
                                .keyType(ColumnSchemaDesc.builder().type("INT8").build())
                                .valueType(ColumnSchemaDesc.builder().name("value").type("INT32").build())
                                .build())))
                .build();
        var newMap = Wal.ColumnSchema.newBuilder()
                .setColumnName("map")
                .setColumnType("MAP")
                .setColumnIndex(this.schema.getColumnSchemaByName("map").getIndex())
                .setKeyType(Wal.ColumnSchema.newBuilder()
                        .setColumnType("INT8")
                        .build());
        assertThat(diff, is(Wal.TableSchema.newBuilder().addColumns(newMap).build()));
        var walMap = this.schema.getColumnSchemaList().stream()
                .collect(Collectors.toMap(ColumnSchema::getName, ColumnSchema::toWal));
        newMap.setKeyType(newMap.getKeyTypeBuilder().setColumnName("key"));
        newMap.setValueType(walMap.get("map").getValueType());
        walMap.put("map", newMap);
        this.schema.update(diff);
        walMap.forEach((k, v) -> assertThat(this.schema.getColumnSchemaByName(k).toWal().build(), is(v.build())));
    }

    @Test
    public void testGetDiffAndUpdateMapValue() {
        var diff = this.schema.getDiff(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder()
                                .name("map")
                                .type("MAP")
                                .keyType(ColumnSchemaDesc.builder().name("key").type("STRING").build())
                                .valueType(ColumnSchemaDesc.builder().type("INT8").build())
                                .build())))
                .build();
        var newMap = Wal.ColumnSchema.newBuilder()
                .setColumnName("map")
                .setColumnType("MAP")
                .setColumnIndex(this.schema.getColumnSchemaByName("map").getIndex())
                .setValueType(Wal.ColumnSchema.newBuilder()
                        .setColumnType("INT8")
                        .build());
        assertThat(diff, is(Wal.TableSchema.newBuilder().addColumns(newMap).build()));
        var walMap = this.schema.getColumnSchemaList().stream()
                .collect(Collectors.toMap(ColumnSchema::getName, ColumnSchema::toWal));
        newMap.setKeyType(walMap.get("map").getKeyType());
        newMap.setValueType(newMap.getValueTypeBuilder().setColumnName("value"));
        walMap.put("map", newMap);
        this.schema.update(diff);
        walMap.forEach((k, v) -> assertThat(this.schema.getColumnSchemaByName(k).toWal().build(), is(v.build())));
    }

    @Test
    public void testGetDiffAndUpdateObjectPythonType() {
        var diff = this.schema.getDiff(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder()
                                .name("obj")
                                .type("OBJECT")
                                .pythonType("tt")
                                .build())))
                .build();
        var newObj = Wal.ColumnSchema.newBuilder()
                .setColumnName("obj")
                .setColumnType("OBJECT")
                .setPythonType("tt")
                .setColumnIndex(this.schema.getColumnSchemaByName("obj").getIndex());
        assertThat(diff, is(Wal.TableSchema.newBuilder().addColumns(newObj).build()));
        var walMap = this.schema.getColumnSchemaList().stream()
                .collect(Collectors.toMap(ColumnSchema::getName, ColumnSchema::toWal));
        newObj.addAllAttributes(walMap.get("obj").getAttributesList());
        walMap.put("obj", newObj);
        this.schema.update(diff);
        walMap.forEach((k, v) -> assertThat(this.schema.getColumnSchemaByName(k).toWal().build(), is(v.build())));
    }

    @Test
    public void testGetDiffAndUpdateObjectAttributes() {
        var diff = this.schema.getDiff(new TableSchemaDesc("k",
                        List.of(ColumnSchemaDesc.builder()
                                .name("obj")
                                .type("OBJECT")
                                .pythonType("t")
                                .attributes(List.of(ColumnSchemaDesc.builder().name("a").type("INT64").build(),
                                        ColumnSchemaDesc.builder().name("b").type("INT64").build(),
                                        ColumnSchemaDesc.builder().name("c").type("INT32").build()))
                                .build())))
                .build();
        var newObj = Wal.ColumnSchema.newBuilder()
                .setColumnName("obj")
                .setColumnType("OBJECT")
                .setColumnIndex(this.schema.getColumnSchemaByName("obj").getIndex())
                .addAttributes(Wal.ColumnSchema.newBuilder()
                        .setColumnName("a")
                        .setColumnType("INT64")
                        .build())
                .addAttributes(Wal.ColumnSchema.newBuilder()
                        .setColumnName("c")
                        .setColumnType("INT32")
                        .build());
        assertThat(diff, is(Wal.TableSchema.newBuilder().addColumns(newObj).build()));
        var walMap = this.schema.getColumnSchemaList().stream()
                .collect(Collectors.toMap(ColumnSchema::getName, ColumnSchema::toWal));
        newObj.setPythonType("t");
        newObj.addAttributes(1, walMap.get("obj").getAttributes(1));
        walMap.put("obj", newObj);
        this.schema.update(diff);
        walMap.forEach((k, v) -> assertThat(this.schema.getColumnSchemaByName(k).toWal().build(), is(v.build())));
    }
}

