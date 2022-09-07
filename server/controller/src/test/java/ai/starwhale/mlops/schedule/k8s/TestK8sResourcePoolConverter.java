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

package ai.starwhale.mlops.schedule.k8s;

import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestK8sResourcePoolConverter {

    @Test
    public void testNormalCase(){
        K8sResourcePoolConverter resourcePoolConverter = new K8sResourcePoolConverter();
        Map<String, String> k8sLabel = resourcePoolConverter.toK8sLabel(
            new ResourcePool("3"));
        Assertions.assertEquals(1,k8sLabel.size());
        Assertions.assertEquals("true",k8sLabel.get("pool.starwhale.ai/3"));

    }

    @Test
    public void testEmptyLabelCase(){
        K8sResourcePoolConverter resourcePoolConverter = new K8sResourcePoolConverter();
        Map<String, String> k8sLabel = resourcePoolConverter.toK8sLabel(
            new ResourcePool());
        Assertions.assertEquals(0,k8sLabel.size());

    }

}
