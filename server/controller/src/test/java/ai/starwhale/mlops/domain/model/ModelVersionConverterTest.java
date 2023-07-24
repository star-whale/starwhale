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

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.job.spec.JobSpecParser;
import ai.starwhale.mlops.domain.job.spec.StepSpec;
import ai.starwhale.mlops.domain.model.converter.ModelVersionVoConverter;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelVersionConverterTest {

    private ModelVersionVoConverter modelVersionVoConverter;

    @BeforeEach
    public void setUp() {
        modelVersionVoConverter = new ModelVersionVoConverter(
                new IdConverter(),
                new VersionAliasConverter(),
                new JobSpecParser());
    }

    @Test
    public void testConvert() {
        var latest = ModelVersionEntity.builder()
                .id(2L)
                .versionName("name1")
                .versionOrder(2L)
                .versionTag("tag1")
                .jobs("default:\n- concurrency: 2")
                .shared(true)
                .build();
        var res = modelVersionVoConverter.convert(ModelVersionEntity.builder()
                .id(1L)
                .versionName("name1")
                .versionOrder(2L)
                .versionTag("tag1")
                .jobs("default:\n- concurrency: 2")
                .shared(true)
                .build(), latest);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("name", is("name1")),
                hasProperty("alias", is("v2")),
                hasProperty("latest", is(false)),
                hasProperty("shared", is(1)),
                hasProperty("tag", is("tag1")),
                hasProperty("stepSpecs",
                        is(List.of(StepSpec.builder().jobName("default").concurrency(2).replicas(1).build())))
        ));
    }

}
