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

package ai.starwhale.mlops.domain.runtime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeConverter;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RuntimeConverterTest {

    private RuntimeConverter convertor;

    @BeforeEach
    public void setUp() {
        convertor = new RuntimeConverter(new IdConverter());
    }

    @Test
    public void testConvert() {
        var res = convertor.convert(null);
        assertThat(res, notNullValue());

        res = convertor.convert(RuntimeEntity.builder()
                .id(1L)
                .runtimeName("rt1")
                .createdTime(new Date())
                .build());
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("name", is("rt1"))
        ));
    }
}
