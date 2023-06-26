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

package ai.starwhale.mlops.common.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelServingTest {
    private ModelServingMapper modelServingMapper;
    private ModelServing modelServing;

    @BeforeEach
    void setUp() {
        modelServingMapper = mock(ModelServingMapper.class);
        modelServing = new ModelServing(modelServingMapper);
    }

    @Test
    void getPrefix() {
        var prefix = modelServing.getPrefix();
        assertEquals("model-serving", prefix);
    }

    @Test
    void getTarget() {
        long id = 1L;
        when(modelServingMapper.find(id)).thenReturn(ModelServingEntity.builder().build());
        var target = modelServing.getTarget("1");
        assertEquals("http://model-serving-1/", target);

        var notFound = String.format("%d/ppl", id + 1);
        var thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> modelServing.getTarget(notFound));
        assertTrue(thrown.getMessage().startsWith("can not find model serving entry"));
    }
}
