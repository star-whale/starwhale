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

package ai.starwhale.mlops.domain.system;

import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.schedule.k8s.K8sResourcePoolConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * test for {@link K8sResourcePoolConverter}
 */
public class ResourcePoolConverterTest {
    private final Map<String, Map<String, String>> tests;

    public ResourcePoolConverterTest() {
        tests = Map.of(
            "foo", Map.of("pool.starwhale.ai/foo", "true"),
            "aLongLongLongLongLongLabel", Map.of("pool.starwhale.ai/aLongLongLongLongLongLabel", "true"),
            "default", new HashMap<>(),
            "", new HashMap<>()
        );
    }

    @Test
    public void testToK8sLabel() {
        var converter = new K8sResourcePoolConverter();
        tests.forEach((pool, k8sLabel) -> {
            var rcPool = ResourcePool.builder().label(pool).build();
            var label = converter.toK8sLabel(rcPool);
            Assertions.assertEquals(k8sLabel, label);
        });
    }

    @Test
    public void testToEntities() {
        var converter = new K8sResourcePoolConverter();
        tests.forEach((pool, k8sLabel) -> {
            var pools = converter.toResourcePools(k8sLabel);
            if (k8sLabel.isEmpty()) {
                Assertions.assertTrue(pools.isEmpty());
                return;
            }
            var entity = ResourcePool.builder().label(pool).build();
            Assertions.assertEquals(1, pools.size());
            Assertions.assertEquals(entity, pools.get(0));
        });

        var emptyEntityLabels = new ArrayList<String>();
        var extra = Map.of(
            Map.of("foo", "bar"), emptyEntityLabels,
            Map.of("foo", "true"), emptyEntityLabels,
            Map.of("pool.starwhale.ai/foo", "bar"), emptyEntityLabels,
            Map.of("pool.starwhale.ai/foo", "true"), List.of("foo"),
            Map.of("wrong.starwhale.ai/foo", "true"), emptyEntityLabels,
            Map.of("pool.starwhale.ai/foo", "true", "pool.starwhale.ai/bar", "xyz"), List.of("foo"),
            Map.of("pool.starwhale.ai/foo", "true", "pool.starwhale.ai/bar", "true"), List.of("foo", "bar")
        );
        extra.forEach((k8sLabel, entityLabels) -> {
            var pools = converter.toResourcePools(k8sLabel);
            var expected = new ArrayList<ResourcePool>();
            entityLabels.forEach(label -> expected.add(ResourcePool.builder().label(label).build()));
            Assertions.assertEquals(expected.size(), pools.size());
            Assertions.assertTrue(expected.containsAll(pools));
        });
    }
}
