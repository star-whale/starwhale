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
import static org.hamcrest.Matchers.nullValue;

import ai.starwhale.mlops.common.DockerImage;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeVersionConverter;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RuntimeVersionConverterTest {

    private RuntimeVersionConverter runtimeVersionConvertor;

    @BeforeEach
    public void setUp() {
        runtimeVersionConvertor = new RuntimeVersionConverter(
                new IdConverter(),
                new VersionAliasConverter(),
                new DockerSetting());
    }

    @Test
    public void testConvert() {
        var entity = RuntimeVersionEntity.builder()
                .id(1L)
                .versionName("name1")
                .versionOrder(2L)
                .versionTag("tag1")
                .versionMeta(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)
                .image("image1")
                .shared(true)
                .build();
        var res = runtimeVersionConvertor.convert(entity);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("name", is("name1")),
                hasProperty("alias", is("v2")),
                hasProperty("latest", is(false)),
                hasProperty("tags", nullValue()), // tag is not set
                hasProperty("meta", is(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)),
                hasProperty("shared", is(true)),
                hasProperty("image", is(RuntimeTestConstants.BUILTIN_IMAGE))
        ));
        assertThat("image", res.getImage(), is(RuntimeTestConstants.BUILTIN_IMAGE));

        res = runtimeVersionConvertor.convert(entity, entity, List.of("tag2"));
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("name", is("name1")),
                hasProperty("alias", is("v2")),
                hasProperty("latest", is(true)),
                hasProperty("tags", is(List.of("tag2"))), // use the tag from parameter
                hasProperty("meta", is(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)),
                hasProperty("shared", is(true)),
                hasProperty("image", is(RuntimeTestConstants.BUILTIN_IMAGE))
        ));
    }

    @Test
    public void testImageParse() {
        var runtime = RuntimeVersionEntity.builder()
                .id(1L)
                .versionName("123456")
                .versionMeta(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)
                .build();
        // case 1: use builtin
        var dockerImage = new DockerImage(runtime.getImage("self.registry:5000"));
        assertThat("registry", dockerImage.getRepo(), is("self.registry:5000"));
        assertThat("image", dockerImage.toString(), is("self.registry:5000/starwhale:0.4.7.builtin"));

        // case 2: use custom
        runtime = RuntimeVersionEntity.builder()
                .id(1L)
                .versionName("123456")
                .versionMeta(RuntimeTestConstants.MANIFEST_WITHOUT_BUILTIN_IMAGE)
                .build();
        dockerImage = new DockerImage(runtime.getImage("self.registry:5000"));
        assertThat("registry", dockerImage.getRepo(), is(RuntimeTestConstants.CUSTOM_REPO));
        assertThat("image", dockerImage.toString(), is(RuntimeTestConstants.CUSTOM_IMAGE));
    }

}
