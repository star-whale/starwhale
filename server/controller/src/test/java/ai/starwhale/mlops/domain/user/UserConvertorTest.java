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

package ai.starwhale.mlops.domain.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserConvertorTest {

    private UserConvertor userConvertor;

    @BeforeEach
    public void setUp() {
        IdConvertor idConvertor = new IdConvertor();
        userConvertor = new UserConvertor(idConvertor);
    }

    @Test
    public void testConvert() {
        var res = userConvertor.convert(null);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("")),
                hasProperty("name", is("")),
                hasProperty("createdTime", is(-1L)),
                hasProperty("isEnabled", is(false))
        ));

        res = userConvertor.convert(UserEntity.builder()
                .id(1L)
                .userName("user1")
                .userEnabled(1)
                .build());

        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("name", is("user1")),
                hasProperty("createdTime", is(-1L)),
                hasProperty("isEnabled", is(true))

        ));

        res = userConvertor.convert(UserEntity.builder()
                .id(2L)
                .userName("user2")
                .userEnabled(0)
                .build());

        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("2")),
                hasProperty("name", is("user2")),
                hasProperty("createdTime", is(-1L)),
                hasProperty("isEnabled", is(false))
        ));
    }

    @Test
    public void testRevert() {
        assertThrows(NullPointerException.class,
                () -> userConvertor.revert(null));

        var res = userConvertor.revert(UserVo.builder()
                .id("1").name("user1").isEnabled(true).build());

        assertThat(res, allOf(
                hasProperty("id", is(1L)),
                hasProperty("userName", is("user1")),
                hasProperty("userEnabled", is(1))
        ));
    }

}
