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

package ai.starwhale.mlops.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.exception.ConvertException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IdConverterTest {

    private IdConverter idConvertor;

    @BeforeEach
    public void setUp() {
        idConvertor = new IdConverter();
    }

    @Test
    public void testConvert() {
        var res = idConvertor.convert((Long) null);
        assertThat(res, nullValue());

        res = idConvertor.convert(11L);
        assertThat(res, is("11"));

        var listRes = idConvertor.convertList(List.of(11L, 22L));
        assertThat(listRes, is(List.of("11", "22")));
    }

    @Test
    public void testRevert() {
        var res = idConvertor.revert((String) null);
        assertThat(res, nullValue());

        res = idConvertor.revert("");
        assertThat(res, nullValue());

        res = idConvertor.revert("123");
        assertThat(res, is(123L));

        assertThrows(ConvertException.class,
                () -> idConvertor.revert("123.4"));

        assertThrows(ConvertException.class,
                () -> idConvertor.revert("a123"));

        var listRes = idConvertor.revertList(List.of("11", "22"));
        assertThat(listRes, is(List.of(11L, 22L)));
    }

    @Test
    public void testIsId() {
        var res = idConvertor.isId(null);
        assertThat(res, is(false));

        res = idConvertor.isId("");
        assertThat(res, is(false));

        res = idConvertor.isId("123");
        assertThat(res, is(true));

        res = idConvertor.isId("a123");
        assertThat(res, is(false));

        res = idConvertor.isId("123.45");
        assertThat(res, is(false));
    }
}
