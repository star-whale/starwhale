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

import ai.starwhale.mlops.api.protocol.user.RoleVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.domain.user.po.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class RoleConvertorTest {

    private RoleConvertor roleConvertor;

    @BeforeEach
    public void setUp() {
        IdConvertor idConvertor = new IdConvertor();
        roleConvertor = new RoleConvertor(idConvertor);
    }

    @Test
    public void testConvert() {
        var res = roleConvertor.convert(null);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("")),
                hasProperty("name", is("")),
                hasProperty("code", is("")),
                hasProperty("description", is(""))
        ));

        res = roleConvertor.convert(RoleEntity.builder()
                .id(1L)
                .roleName("Owner")
                .roleCode("OWNER")
                .roleDescription("admin")
                .build());
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("name", is("Owner")),
                hasProperty("code", is("OWNER")),
                hasProperty("description", is("admin"))
        ));
    }

    @Test
    public void testRevert() {
        assertThrows(UnsupportedOperationException.class, () -> roleConvertor.revert(RoleVo.builder().build()));
    }
}
