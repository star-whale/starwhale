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

package ai.starwhale.mlops.domain.job;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.api.protocol.runtime.BaseImageVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.job.po.BaseImageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BaseImageConverterTest {

    private BaseImageConverter baseImageConvertor;

    @BeforeEach
    public void setUp() {
        baseImageConvertor = new BaseImageConverter(new IdConverter());
    }

    @Test
    public void testConvert() {
        var res = baseImageConvertor.convert(null);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("")),
                hasProperty("name", is(""))
        ));

        res = baseImageConvertor.convert(BaseImageEntity.builder()
                .id(1L)
                .imageName("image1")
                .build());
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("name", is("image1"))
        ));
    }

    @Test
    public void testRevert() {
        var res = baseImageConvertor.revert(BaseImageVo.builder()
                .id("2")
                .name("image2")
                .build());
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is(2L)),
                hasProperty("imageName", is("image2"))
        ));

        assertThrows(NullPointerException.class,
                () -> baseImageConvertor.revert(null));
    }
}
