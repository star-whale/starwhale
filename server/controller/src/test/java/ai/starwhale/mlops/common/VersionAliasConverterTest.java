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
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.exception.ConvertException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VersionAliasConverterTest {

    private VersionAliasConverter versionAliasConvertor;

    @BeforeEach
    public void setUp() {
        versionAliasConvertor = new VersionAliasConverter();
    }

    @Test
    public void testConvert() {
        assertThrows(ConvertException.class,
                () -> versionAliasConvertor.convert(null));

        var res = versionAliasConvertor.convert(1L);
        assertThat(res, is("v1"));

        res = versionAliasConvertor.convert(100L);
        assertThat(res, is("v100"));
    }

    @Test
    public void testRevert() {
        var res = versionAliasConvertor.revert("v1");
        assertThat(res, is(1L));

        res = versionAliasConvertor.revert("v100");
        assertThat(res, is(100L));

        assertThrows(ConvertException.class,
                () -> versionAliasConvertor.revert(null));

        assertThrows(ConvertException.class,
                () -> versionAliasConvertor.revert(""));

        assertThrows(ConvertException.class,
                () -> versionAliasConvertor.revert("v"));

        assertThrows(ConvertException.class,
                () -> versionAliasConvertor.revert("vv100"));
    }

    @Test
    public void testIsVersionAlias() {
        var res = versionAliasConvertor.isVersionAlias(null);
        assertThat(res, is(false));

        res = versionAliasConvertor.isVersionAlias("");
        assertThat(res, is(false));

        res = versionAliasConvertor.isVersionAlias("v");
        assertThat(res, is(false));

        res = versionAliasConvertor.isVersionAlias("va");
        assertThat(res, is(false));

        res = versionAliasConvertor.isVersionAlias("v2");
        assertThat(res, is(true));

        res = versionAliasConvertor.isLatest("v2");
        assertThat(res, is(false));

        res = versionAliasConvertor.isLatest("latest");
        assertThat(res, is(true));

        res = versionAliasConvertor.isLatest("LATEST");
        assertThat(res, is(true));
    }
}
