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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ColumnTypeTupleTest {

    @Test
    public void testGetTypeName() {
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).getTypeName(), is("TUPLE"));
    }

    @Test
    public void testToColumnSchemaDesc() {
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).toColumnSchemaDesc("t"),
                is(ColumnSchemaDesc.builder()
                        .name("t")
                        .type("TUPLE")
                        .elementType(ColumnSchemaDesc.builder()
                                .type("INT32")
                                .build())
                        .build()));
    }

    @Test
    public void testToString() {
        assertThat(new ColumnTypeList(ColumnTypeScalar.INT32).toString(), is("[INT32]"));
    }


    @Test
    public void testIsComparableWith() {
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).isComparableWith(ColumnTypeScalar.UNKNOWN), is(true));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).isComparableWith(ColumnTypeScalar.INT32), is(false));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).isComparableWith(
                        new ColumnTypeTuple(ColumnTypeScalar.INT32)),
                is(true));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).isComparableWith(
                        new ColumnTypeTuple(ColumnTypeScalar.FLOAT64)),
                is(true));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).isComparableWith(
                        new ColumnTypeTuple(ColumnTypeScalar.STRING)),
                is(false));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).isComparableWith(
                        new ColumnTypeList(ColumnTypeScalar.INT32)),
                is(true));
    }

    @Test
    public void testEncode() {
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).encode(List.of(9, 10, 11), false),
                is(List.of("00000009", "0000000a", "0000000b")));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).encode(List.of(9, 10, 11), true),
                is(List.of("9", "10", "11")));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).encode(new ArrayList<Integer>() {
                    {
                        add(0);
                        add(null);
                        add(1);
                    }
                }, false),
                is(new ArrayList<String>() {
                    {
                        add("00000000");
                        add(null);
                        add("00000001");
                    }
                }));
        var composite = new ColumnTypeTuple(
                new ColumnTypeObject("t", Map.of("a", ColumnTypeScalar.INT32, "b", ColumnTypeScalar.INT32)));
        assertThat(composite.encode(List.of(Map.of("a", 9, "b", 10), Map.of("a", 10, "b", 11)), false),
                is(List.of(Map.of("a", "00000009", "b", "0000000a"), Map.of("a", "0000000a", "b", "0000000b"))));
        assertThat(composite.encode(List.of(Map.of("a", 9, "b", 10), Map.of("a", 10, "b", 11)), true),
                is(List.of(Map.of("a", "9", "b", "10"), Map.of("a", "10", "b", "11"))));
    }

    @Test
    public void testDecode() {
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).decode(List.of("9", "a", "b")),
                is(List.of(9, 10, 11)));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).decode(new ArrayList<String>() {
                    {
                        add("0");
                        add(null);
                        add("1");
                    }
                }),
                is(new ArrayList<Integer>() {
                    {
                        add(0);
                        add(null);
                        add(1);
                    }
                }));
        var composite = new ColumnTypeTuple(
                new ColumnTypeObject("t", Map.of("a", ColumnTypeScalar.INT32, "b", ColumnTypeScalar.INT32)));
        assertThat(composite.decode(List.of(Map.of("a", "9", "b", "a"), Map.of("a", "a", "b", "b"))),
                is(List.of(Map.of("a", 9, "b", 10), Map.of("a", 10, "b", 11))));

        assertThrows(SwValidationException.class, () -> new ColumnTypeTuple(ColumnTypeScalar.INT32).decode("9"));
        assertThrows(SwValidationException.class,
                () -> new ColumnTypeTuple(ColumnTypeScalar.INT32).decode(List.of("z")));
    }

    @Test
    public void testFromAndToWal() {
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).toWal(-1, List.of(9, 10, 11)).getIndex(), is(-1));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).toWal(10, List.of(9, 10, 11)).getIndex(), is(10));
        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).fromWal(
                        new ColumnTypeTuple(ColumnTypeScalar.INT32).toWal(0, null).build()),
                nullValue());

        assertThat(new ColumnTypeTuple(ColumnTypeScalar.INT32).fromWal(
                        new ColumnTypeTuple(ColumnTypeScalar.INT32).toWal(0, List.of(9, 10, 11)).build()),
                is(List.of(9, 10, 11)));
    }

}
