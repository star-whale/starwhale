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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.exception.SwValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ColumnTypeVirtualTest {

    @Nested
    public class VirtualObjectTest {
        @Test
        public void testBuild() {
            assertThat("build",
                    ColumnTypeVirtual.build("x-y", "$.x.y",
                        (name) -> new ColumnSchema(ColumnSchemaDesc.builder()
                            .name("x")
                            .type(ColumnTypeObject.TYPE_NAME)
                            .pythonType("dict")
                            .attributes(List.of(
                                ColumnSchemaDesc.builder().name("y").type(ColumnTypeScalar.INT32.getTypeName()).build(),
                                ColumnSchemaDesc.builder().name("z").type(ColumnTypeScalar.INT32.getTypeName()).build()
                            )).build(), 0)),
                    is(new ColumnTypeVirtual("x-y", "x.y", ColumnTypeScalar.INT32)));

            assertThrows(SwValidationException.class,
                    () -> ColumnTypeVirtual.build("x-y", "$.x.y.z",
                        (name) -> new ColumnSchema(ColumnSchemaDesc.builder()
                            .name("x")
                            .type(ColumnTypeObject.TYPE_NAME)
                            .pythonType("dict")
                            .attributes(List.of(
                                ColumnSchemaDesc.builder().name("y").type(ColumnTypeScalar.INT32.getTypeName()).build(),
                                ColumnSchemaDesc.builder().name("z").type(ColumnTypeScalar.INT32.getTypeName()).build()
                            )).build(), 0))
            );
        }

        @Test
        public void testParseFromValues() {
            assertThat("parse value",
                    new ColumnTypeVirtual("x", "x", ColumnTypeScalar.INT32).parseFromValues(Map.of(
                        "x", Map.of("y", 1, "z", "val-z"))),
                    is(Map.of("y", 1, "z", "val-z")));

            assertThat("parse value",
                    new ColumnTypeVirtual("x-y", "x.y", ColumnTypeScalar.INT32).parseFromValues(Map.of(
                    "x", Map.of("y", 1, "z", "val-z"))),
                    is(1));

            assertThat("parse null",
                    new ColumnTypeVirtual("x-y", "", ColumnTypeScalar.INT32).parseFromValues(null),
                    nullValue());

            assertThrows(UnsupportedOperationException.class,
                    () -> new ColumnTypeVirtual("x-y", "x.y", ColumnTypeScalar.INT32)
                            .parseFromValues(Map.of("x", List.of("y", "z"))));
        }
    }

    @Nested
    public class VirtualMapTest {
        @Test
        public void testBuild() {
            assertThat("build",
                    ColumnTypeVirtual.build("x-y", "$.x.y",
                        (name) -> new ColumnSchema(ColumnSchemaDesc.builder()
                            .name("x")
                            .type(ColumnTypeMap.TYPE_NAME)
                            .keyType(ColumnTypeScalar.STRING.toColumnSchemaDesc("xx"))
                            .valueType(ColumnTypeScalar.INT32.toColumnSchemaDesc("yy"))
                            .build(), 0)),
                    is(new ColumnTypeVirtual("x-y", "x.y", ColumnTypeScalar.INT32)));
        }

        @Test
        public void testParseFromValues() {
            assertThat("parse value",
                    new ColumnTypeVirtual("x", "x", ColumnTypeScalar.INT32).parseFromValues(Map.of(
                        "x", Map.of("y", 1, "z", "val-z"))),
                    is(Map.of("y", 1, "z", "val-z")));

            assertThat("parse value",
                    new ColumnTypeVirtual("x-y", "x.y", ColumnTypeScalar.INT32).parseFromValues(Map.of(
                    "x", Map.of("y", 1, "z", "val-z"))),
                    is(1));
        }
    }

    @Nested
    public class VirtualListTest {
        @Test
        public void testBuild() {
            assertThat("build",
                    ColumnTypeVirtual.build("x-0", "$.x[0]",
                        (name) -> new ColumnSchema(ColumnSchemaDesc.builder()
                            .name("x")
                            .type(ColumnTypeList.TYPE_NAME)
                            .elementType(
                                ColumnSchemaDesc.builder()
                                    .type(ColumnTypeScalar.INT32.getTypeName())
                                    .build()
                            )
                            .build(), 0)),
                    is(new ColumnTypeVirtual("x-0", "x[0]", ColumnTypeScalar.INT32)));

            assertThat("build",
                    ColumnTypeVirtual.build("x-0-y", "$.x[0].y",
                        (name) -> new ColumnSchema(ColumnSchemaDesc.builder()
                            .name("x")
                            .type(ColumnTypeList.TYPE_NAME)
                            .elementType(
                                ColumnSchemaDesc.builder()
                                    .type(ColumnTypeObject.TYPE_NAME)
                                    .pythonType("dict")
                                    .attributes(List.of(
                                        ColumnSchemaDesc.builder()
                                            .name("y")
                                            .type(ColumnTypeList.TYPE_NAME)
                                            .elementType(
                                                ColumnSchemaDesc.builder()
                                                    .type(ColumnTypeScalar.INT32.getTypeName())
                                                    .build()
                                            ).build(),
                                        ColumnSchemaDesc.builder()
                                            .name("z")
                                            .type(ColumnTypeScalar.INT32.getTypeName())
                                            .build()
                                    )).build()

                            ).build(), 0)),
                    is(new ColumnTypeVirtual("x-0-y", "x[0].y",
                            ColumnTypeList.fromColumnSchemaDesc(ColumnSchemaDesc.builder()
                                    .name("y")
                                    .type(ColumnTypeList.TYPE_NAME)
                                    .elementType(
                                        ColumnSchemaDesc.builder()
                                            .type(ColumnTypeScalar.INT32.getTypeName())
                                            .build()
                                    ).build()))));

            assertThat("build",
                    ColumnTypeVirtual.build("x-y-0", "$.x.y[0]",
                        (name) -> new ColumnSchema(ColumnSchemaDesc.builder()
                            .name("x")
                            .type(ColumnTypeObject.TYPE_NAME)
                            .pythonType("dict")
                            .attributes(List.of(
                                ColumnSchemaDesc.builder()
                                    .name("y")
                                    .type(ColumnTypeList.TYPE_NAME)
                                    .elementType(
                                        ColumnSchemaDesc.builder().type(ColumnTypeScalar.INT32.getTypeName()).build())
                                    .build(),
                                ColumnSchemaDesc.builder()
                                    .name("yy")
                                    .type(ColumnTypeScalar.INT32.getTypeName())
                                    .build()
                            ))
                            .build(), 0)),
                    is(new ColumnTypeVirtual("x-y-0", "x.y[0]", ColumnTypeScalar.INT32)));

            assertThat("build for scalar",
                    ColumnTypeVirtual.build("x-y-0-z", "$.x.y[0].z",
                        (name) -> new ColumnSchema(ColumnSchemaDesc.builder()
                            .name("x")
                            .type(ColumnTypeObject.TYPE_NAME)
                            .pythonType("dict")
                            .attributes(List.of(
                                ColumnSchemaDesc.builder()
                                    .name("y")
                                    .type(ColumnTypeList.TYPE_NAME)
                                    .elementType(
                                        ColumnSchemaDesc.builder()
                                            .type(ColumnTypeObject.TYPE_NAME)
                                            .pythonType("dict")
                                            .attributes(List.of(
                                                ColumnSchemaDesc.builder()
                                                    .name("z")
                                                    .type(ColumnTypeScalar.STRING.getTypeName())
                                                    .build()
                                            )).build()
                                    )
                                    .build()
                            ))
                            .build(), 0)),
                    is(new ColumnTypeVirtual("x-y-0-z", "x.y[0].z", ColumnTypeScalar.STRING)));

            assertThat("build for object",
                    ColumnTypeVirtual.build("x-y-0-z", "$.x.y[0].z",
                        (name) -> new ColumnSchema(ColumnSchemaDesc.builder()
                            .name("x")
                            .type(ColumnTypeObject.TYPE_NAME)
                            .pythonType("dict")
                            .attributes(List.of(
                                ColumnSchemaDesc.builder()
                                    .name("y")
                                    .type(ColumnTypeList.TYPE_NAME)
                                    .elementType(
                                        ColumnSchemaDesc.builder()
                                            .type(ColumnTypeObject.TYPE_NAME)
                                            .pythonType("dict")
                                            .attributes(List.of(
                                                ColumnSchemaDesc.builder()
                                                    .name("z")
                                                    .type(ColumnTypeList.TYPE_NAME)
                                                    .elementType(
                                                        ColumnSchemaDesc.builder()
                                                            .type(ColumnTypeScalar.STRING.getTypeName())
                                                            .build()
                                                    )
                                                    .build()
                                            ))
                                            .build()
                                    )
                                    .build()
                            ))
                            .build(), 0)),
                    is(new ColumnTypeVirtual("x-y-0-z", "x.y[0].z",
                            ColumnTypeObject.fromColumnSchemaDesc(ColumnSchemaDesc.builder()
                                .name("z")
                                .type(ColumnTypeList.TYPE_NAME)
                                .elementType(
                                    ColumnSchemaDesc.builder()
                                        .type(ColumnTypeScalar.STRING.getTypeName())
                                        .build()
                                )
                                .build()))));

            assertThat("build for object",
                    ColumnTypeVirtual.build("x-y-0-z-0-k", "$.x.y[0].z[0].k",
                        (name) -> new ColumnSchema(ColumnSchemaDesc.builder()
                            .name("x")
                            .type(ColumnTypeObject.TYPE_NAME)
                            .pythonType("dict")
                            .attributes(List.of(
                                ColumnSchemaDesc.builder()
                                    .name("y")
                                    .type(ColumnTypeList.TYPE_NAME)
                                    .elementType(
                                        ColumnSchemaDesc.builder()
                                            .type(ColumnTypeObject.TYPE_NAME)
                                            .pythonType("dict")
                                            .attributes(List.of(
                                                ColumnSchemaDesc.builder()
                                                    .name("z")
                                                    .type(ColumnTypeList.TYPE_NAME)
                                                    .elementType(
                                                        ColumnSchemaDesc.builder()
                                                            .type(ColumnTypeObject.TYPE_NAME)
                                                            .pythonType("dict")
                                                            .attributes(List.of(
                                                                ColumnSchemaDesc.builder()
                                                                    .name("k")
                                                                    .type(ColumnTypeScalar.STRING.getTypeName())
                                                                    .build()
                                                            ))
                                                            .build()
                                                    )
                                                    .build()
                                            ))
                                            .build()
                                    )
                                    .build()
                            ))
                            .build(), 0)),
                    is(new ColumnTypeVirtual("x-y-0-z-0-k", "x.y[0].z[0].k", ColumnTypeScalar.STRING)));
        }

        @Test
        public void testParseFromValues() {
            assertThat("parse value",
                    new ColumnTypeVirtual("x-0", "x[0]", ColumnTypeScalar.INT32).parseFromValues(
                        Map.of("x", List.of("1", "2"))),
                    is("1"));

            assertThat("parse value",
                    new ColumnTypeVirtual("x-0", "x[0]", ColumnTypeScalar.STRING).parseFromValues(
                        Map.of("x", List.of("v1", "v2"))),
                    is("v1"));

            assertThat("parse value",
                    new ColumnTypeVirtual("x-y-0", "x.y[0]", ColumnTypeScalar.STRING).parseFromValues(
                        Map.of("x", Map.of("y", List.of("v1", "v2")))),
                    is("v1"));

            assertThat("parse value",
                    new ColumnTypeVirtual("x-y-0", "x.y[0].z1", ColumnTypeScalar.STRING).parseFromValues(
                        Map.of(
                            "x", Map.of(
                                "y", List.of(Map.of("z1", "z1-val"), Map.of("z2", "z2-val"))))
                    ),
                    is("z1-val"));

            assertThat("parse value",
                    new ColumnTypeVirtual("x-y-0", "x.y[0].z[0].k", ColumnTypeScalar.STRING).parseFromValues(
                        Map.of("x", Map.of(
                                "y", List.of(
                                    Map.of("z", List.of(Map.of("k", "z1-val"))),
                                    Map.of("z", List.of(Map.of("k", "z2-val"))))
                            ))
                    ),
                    is("z1-val"));

            assertThat("parse value",
                    new ColumnTypeVirtual("x-y", "x.y", new ColumnTypeList(ColumnTypeScalar.STRING))
                            .parseFromValues(Map.of("x", Map.of("y", List.of("v1", "v2")))),
                    is(List.of("v1", "v2")));

            assertThrows(UnsupportedOperationException.class,
                    () -> new ColumnTypeVirtual("x-y-v1", "x.y.v1",
                            new ColumnTypeList(ColumnTypeScalar.STRING)).parseFromValues(
                                Map.of("x", Map.of("y", List.of("v1", "v2")))));
        }
    }

    private ColumnTypeVirtual columnTypeVirtual;

    @BeforeEach
    public void setUp() {
        this.columnTypeVirtual = new ColumnTypeVirtual("x-y", "x.y", ColumnTypeScalar.INT32);
    }

    @Test
    public void testGetTypeName() {
        assertThat(this.columnTypeVirtual.getTypeName(), is("VIRTUAL"));
    }

    @Test
    public void testToColumnSchemaDesc() {
        var columnSchemaDesc = this.columnTypeVirtual.toColumnSchemaDesc("t");
        assertThat(columnSchemaDesc.getName(), is("t"));
        assertThat(columnSchemaDesc.getType(), is("VIRTUAL"));
        assertThat(columnSchemaDesc.getPythonType(), nullValue());
        assertThat(columnSchemaDesc.getElementType(), is(
                ColumnSchemaDesc.builder().type(ColumnTypeScalar.INT32.getTypeName()).build()));
    }

    @Test
    public void testEncode() {
        assertThat(this.columnTypeVirtual.encode(null, false), nullValue());
        assertThat(this.columnTypeVirtual.encode(null, true), nullValue());
        assertThat(this.columnTypeVirtual.encode(8, false), is("00000008"));
    }

}
