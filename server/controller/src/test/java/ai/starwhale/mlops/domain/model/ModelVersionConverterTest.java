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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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
                new JobSpecParser(new YAMLMapper()));
    }

    @Test
    public void testConvert() {
        ModelVersionEntity entity1 = ModelVersionEntity.builder()
                .id(1L)
                .versionName("name1")
                .versionOrder(2L)
                .versionTag("tag1")
                .versionMeta("meta1")
                .manifest("manifest1")
                .evalJobs("default:\n- concurrency: 2")
                .build();
        ModelVersionEntity entity2 = ModelVersionEntity.builder()
                .id(2L)
                .versionName("name2")
                .versionOrder(3L)
                .versionTag("tag2")
                .versionMeta("meta2")
                .manifest("manifest2")
                .evalJobs("default:\n- concurrency: 2")
                .build();
        var res = modelVersionVoConverter.convert(entity1, null);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("name", is("name1")),
                hasProperty("alias", is("v2")),
                hasProperty("tag", is("tag1")),
                hasProperty("meta", is("meta1")),
                hasProperty("manifest", is("manifest1")),
                hasProperty("stepSpecs",
                        is(List.of(StepSpec.builder().concurrency(2).taskNum(1).build())))
        ));

        res = modelVersionVoConverter.convert(entity1, entity2);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("name", is("name1")),
                hasProperty("alias", is("v2")),
                hasProperty("tag", is("tag1")),
                hasProperty("meta", is("meta1")),
                hasProperty("manifest", is("manifest1")),
                hasProperty("stepSpecs",
                        is(List.of(StepSpec.builder().concurrency(2).taskNum(1).build())))
        ));

        res = modelVersionVoConverter.convert(entity2, entity2);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("name", is("name2")),
                hasProperty("alias", is("latest")),
                hasProperty("tag", is("tag2")),
                hasProperty("meta", is("meta2")),
                hasProperty("manifest", is("manifest2")),
                hasProperty("stepSpecs",
                        is(List.of(StepSpec.builder().concurrency(2).taskNum(1).build())))
        ));
    }

}
