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

package ai.starwhale.mlops.domain.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.api.protocol.model.ModelVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.model.po.ModelEntity;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelConvertorTest {

    private ModelConvertor convertor;


    @BeforeEach
    public void setUp() {
        UserConvertor userConvertor = mock(UserConvertor.class);
        given(userConvertor.convert(any())).willReturn(UserVo.empty());
        convertor = new ModelConvertor(new IdConvertor(), userConvertor);
    }

    @Test
    public void testConvert() {
        var res = convertor.convert(null);
        assertThat(res, notNullValue());

        res = convertor.convert(ModelEntity.builder()
                .id(1L)
                .modelName("swmp1")
                .owner(UserEntity.builder().build())
                .createdTime(new Date())
                .build());
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("name", is("swmp1"))
        ));
    }

    @Test
    public void testRevert() {
        assertThrows(UnsupportedOperationException.class,
                () -> convertor.revert(ModelVo.empty()));
    }
}
