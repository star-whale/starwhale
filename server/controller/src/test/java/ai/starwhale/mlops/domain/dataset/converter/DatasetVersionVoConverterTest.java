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

package ai.starwhale.mlops.domain.dataset.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatasetVersionVoConverterTest {

    private DatasetVersionVoConverter datasetVersionVoConverter;

    @BeforeEach
    public void setUp() {
        datasetVersionVoConverter = new DatasetVersionVoConverter(
                new IdConverter(),
                new VersionAliasConverter()
        );
    }

    @Test
    public void testConvert() {
        var latest = DatasetVersionEntity.builder()
                .id(2L)
                .versionOrder(2L)
                .build();
        var res = datasetVersionVoConverter.convert(
                DatasetVersionEntity.builder()
                        .id(1L)
                        .shared(true)
                        .versionName("name1")
                        .versionOrder(2L)
                        .indexTable("table1")
                        .versionMeta("meta1")
                        .build(),
                latest,
                List.of("the outer tag")
        );
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("name", is("name1")),
                hasProperty("shared", is(true)),
                hasProperty("alias", is("v2")),
                hasProperty("tagsList", is(List.of("the outer tag"))),
                hasProperty("meta", is("meta1")),
                hasProperty("latest", is(false))
        ));
    }
}
