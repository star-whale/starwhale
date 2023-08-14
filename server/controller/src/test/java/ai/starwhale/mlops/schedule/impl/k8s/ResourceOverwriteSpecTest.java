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

package ai.starwhale.mlops.schedule.impl.k8s;

import ai.starwhale.mlops.api.protobuf.Model.RuntimeResource;
import ai.starwhale.mlops.schedule.impl.k8s.ResourceOverwriteSpec;
import io.kubernetes.client.custom.Quantity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResourceOverwriteSpecTest {

    @Test
    public void testRuntimeResource() {
        ResourceOverwriteSpec resourceOverwriteSpec = new ResourceOverwriteSpec(
                List.of(
                        RuntimeResource.newBuilder().setType("cpu").setRequest(1.99f).setLimit(1.99f).build(),
                        RuntimeResource.newBuilder().setType("nvidia.com/gpu").setRequest(1.99f).setLimit(2.99f).build()
                ));
        Assertions.assertEquals(
                new Quantity("2"),
                resourceOverwriteSpec.getResourceSelector().getRequests().get("nvidia.com/gpu")
        );
        Assertions.assertEquals(
                new Quantity("3"),
                resourceOverwriteSpec.getResourceSelector().getLimits().get("nvidia.com/gpu")
        );
        Assertions.assertEquals(
                new Quantity("1.99"),
                resourceOverwriteSpec.getResourceSelector().getRequests().get("cpu")
        );

        // test no-k8s resource without limit
        resourceOverwriteSpec = new ResourceOverwriteSpec(
                List.of(
                        RuntimeResource.newBuilder().setType("cpu").setRequest(1.99f).build(),
                        RuntimeResource.newBuilder().setType("nvidia.com/gpu").setRequest(2.f).build()
                ));
        Assertions.assertEquals(
                new Quantity("2"),
                resourceOverwriteSpec.getResourceSelector().getRequests().get("nvidia.com/gpu")
        );
        Assertions.assertEquals(
                new Quantity("2"),
                resourceOverwriteSpec.getResourceSelector().getLimits().get("nvidia.com/gpu")
        );
        Assertions.assertEquals(
                new Quantity("1.99"),
                resourceOverwriteSpec.getResourceSelector().getRequests().get("cpu")
        );
        Assertions.assertNull(resourceOverwriteSpec.getResourceSelector().getLimits().get("cpu"));
    }

}
