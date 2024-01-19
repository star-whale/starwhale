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


import ai.starwhale.mlops.datastore.type.BaseValue;
import ai.starwhale.mlops.datastore.type.StringValue;
import org.junit.jupiter.api.Test;

class TombstoneTest {
    @Test
    public void testKeyDeletedWithPrefix() {
        var tombstone = Tombstone.builder().keyPrefix("foo").build();
        for (var item : new String[]{"foo", "foobar", "foobaz"}) {
            assert tombstone.keyDeleted(new StringValue(item));
        }
        for (var item : new String[]{"bar", "baz", "barfoo"}) {
            assert !tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert !tombstone.keyDeleted(item);
        }
    }

    @Test
    public void testKeyDeletedWithRange() {
        var tombstone = Tombstone.builder()
                .start(new StringValue("foo1"))
                .end(new StringValue("foo9"))
                .startInclusive(true)
                .endInclusive(true)
                .build();
        for (var item : new String[]{"foo1", "foo2", "foo3", "foo4", "foo5", "foo6", "foo7", "foo8", "foo9"}) {
            assert tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new String[]{"foo", "foo0", "fooa", "bar", "baz", "barfoo"}) {
            assert !tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert !tombstone.keyDeleted(item);
        }

        tombstone = Tombstone.builder()
                .start(new StringValue("foo1"))
                .end(new StringValue("foo9"))
                .startInclusive(false)
                .endInclusive(false)
                .build();
        for (var item : new String[]{"foo2", "foo3", "foo4", "foo5", "foo6", "foo7", "foo8"}) {
            assert tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new String[]{"foo1", "foo9", "foo", "foo0", "fooa", "bar", "baz", "barfoo"}) {
            assert !tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert !tombstone.keyDeleted(item);
        }

        tombstone = Tombstone.builder()
                .start(new StringValue("foo1"))
                .end(new StringValue("foo9"))
                .startInclusive(false)
                .endInclusive(true)
                .build();
        for (var item : new String[]{"foo2", "foo3", "foo4", "foo5", "foo6", "foo7", "foo8", "foo9"}) {
            assert tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new String[]{"foo1", "foo", "foo0", "fooa", "bar", "baz", "barfoo"}) {
            assert !tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert !tombstone.keyDeleted(item);
        }

        tombstone = Tombstone.builder()
                .start(new StringValue("foo1"))
                .end(new StringValue("foo9"))
                .startInclusive(true)
                .endInclusive(false)
                .build();

        for (var item : new String[]{"foo1", "foo2", "foo3", "foo4", "foo5", "foo6", "foo7", "foo8"}) {
            assert tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new String[]{"foo9", "foo", "foo0", "fooa", "bar", "baz", "barfoo"}) {
            assert !tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert !tombstone.keyDeleted(item);
        }

        tombstone = Tombstone.builder()
                .start(new StringValue("foo1"))
                .end(new StringValue("foo1"))
                .startInclusive(true)
                .endInclusive(true)
                .build();

        for (var item : new String[]{"foo1"}) {
            assert tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new String[]{"foo", "foo0", "foo2", "fooa", "bar", "baz", "barfoo"}) {
            assert !tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert !tombstone.keyDeleted(item);
        }

        tombstone = Tombstone.builder()
                .start(new StringValue("foo1"))
                .end(new StringValue("foo1"))
                .startInclusive(false)
                .endInclusive(false)
                .build();

        for (var item : new String[]{"foo1", "foo", "foo0", "foo2", "fooa", "bar", "baz", "barfoo"}) {
            assert !tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert !tombstone.keyDeleted(item);
        }

        // none, none
        tombstone = Tombstone.builder().build();
        for (var item : new String[]{"foo1", "foo", "foo0", "foo2", "fooa", "bar", "baz", "barfoo"}) {
            assert tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert tombstone.keyDeleted(item);
        }

        // start, none
        tombstone = Tombstone.builder().start(new StringValue("foo1")).startInclusive(true).build();
        for (var item : new String[]{"foo1", "foo2", "foo3", "foo4", "foo5", "foo6", "foo7", "foo8", "foo9", "fooa",
                "x"}) {
            assert tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new String[]{"foo", "foo0", "bar", "baz", "barfoo"}) {
            assert !tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert !tombstone.keyDeleted(item);
        }

        // none, end
        tombstone = Tombstone.builder().end(new StringValue("foo9")).endInclusive(true).build();
        for (var item : new String[]{"a", "foo1", "foo2", "foo3", "foo4", "foo5", "foo6", "foo7", "foo8", "foo9"}) {
            assert tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new String[]{"fooa", "x"}) {
            assert !tombstone.keyDeleted(new StringValue(item));
        }

        for (var item : new BaseValue[]{BaseValue.valueOf(1), BaseValue.valueOf(1.0), BaseValue.valueOf(true)}) {
            assert !tombstone.keyDeleted(item);
        }
    }
}
