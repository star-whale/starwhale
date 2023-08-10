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

package ai.starwhale.mlops.domain.system.resourcepool.bo;


import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.api.protobuf.Model.RuntimeResource;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourcePoolTest {
    private ResourcePool resourcePool;

    @BeforeEach
    void setUp() {
        var resources = List.of(new Resource("cpu", 3f, 1f, 2f), new Resource("memory", 7f, 5f, 6f));
        resourcePool = ResourcePool.builder().resources(resources).build();
    }

    @Test
    void validateResources() {
        var rr = RuntimeResource.newBuilder().setType("cpu").setRequest(2f);
        resourcePool.validateResource(rr.build());

        // cpu more than max
        rr.setRequest(4f);
        assertThrows(IllegalArgumentException.class, () -> resourcePool.validateResource(rr.build()));
        // cpu less than min
        rr.setRequest(0f);
        assertThrows(IllegalArgumentException.class, () -> resourcePool.validateResource(rr.build()));

        rr.setType("memory");
        rr.setRequest(6f);
        resourcePool.validateResource(rr.build());

        // resource type is empty
        rr.setType("");
        rr.setRequest(1f);
        assertThrows(IllegalArgumentException.class, () -> resourcePool.validateResource(rr.build()));

        // memory more than max
        rr.setRequest(8f);
        assertThrows(IllegalArgumentException.class, () -> resourcePool.validateResource(rr.build()));
        // memory less than min
        rr.setRequest(4f);
        assertThrows(IllegalArgumentException.class, () -> resourcePool.validateResource(rr.build()));

        // unsupported resource type
        rr.setType("foo");
        assertThrows(IllegalArgumentException.class, () -> resourcePool.validateResource(rr.build()));
        rr.setType("gpu");
        assertThrows(IllegalArgumentException.class, () -> resourcePool.validateResource(rr.build()));

        // resource pool has no rules for resource type
        rr.setType("nvidia.com/gpu");
        assertThrows(IllegalArgumentException.class, () -> resourcePool.validateResource(rr.build()));
    }

    @Test
    void patchResources() {
        var cpu = RuntimeResource.newBuilder().setType("cpu").setRequest(2f).build();
        var cpuResources = List.of(cpu);

        // request set (what ever it is) should not be patched
        var ret = resourcePool.patchResources(cpuResources);
        Assertions.assertEquals(List.of(RuntimeResource.newBuilder().setType("cpu").setRequest(2f).build(),
                RuntimeResource.newBuilder().setType("memory").setRequest(6f).build()), ret);
        cpuResources = List.of(cpu.toBuilder().setRequest(4f).build());
        ret = resourcePool.patchResources(cpuResources);
        Assertions.assertEquals(List.of(RuntimeResource.newBuilder().setType("cpu").setRequest(4f).build(),
                RuntimeResource.newBuilder().setType("memory").setRequest(6f).build()), ret);

        // request not set should be patched with default value
        cpuResources = List.of(cpu.toBuilder().setType("cpu").clearRequest().build());
        ret = resourcePool.patchResources(cpuResources);
        Assertions.assertEquals(List.of(RuntimeResource.newBuilder().setType("cpu").setRequest(2f).build(),
                RuntimeResource.newBuilder().setType("memory").setRequest(6f).build()), ret);

        // null resources
        ret = resourcePool.patchResources(null);
        Assertions.assertEquals(List.of(RuntimeResource.newBuilder().setType("cpu").setRequest(2f).build(),
                RuntimeResource.newBuilder().setType("memory").setRequest(6f).build()), ret);

        // no default value in the resource pool settings
        resourcePool = ResourcePool.builder().resources(List.of(new Resource("cpu"))).build();
        ret = resourcePool.patchResources(null);
        Assertions.assertTrue(ret.isEmpty());

        var gpu = RuntimeResource.newBuilder().setType("gpu").setRequest(2f).build();
        var gpuResources = List.of(gpu);
        // request gpu, but pool does not have gpu, should not be patched
        ret = resourcePool.patchResources(gpuResources);
        Assertions.assertEquals(List.of(), ret);
    }

    @Test
    public void marshalAndUnMarshall() throws IOException {
        var resources = List.of(new Resource("cpu", 3f, 1f, 2f), new Resource("memory", 7f, 5f, 6f));
        resourcePool = ResourcePool.builder().resources(resources).build();
        var json = resourcePool.toJson();
        var unmarshalled = ResourcePool.fromJson(json);
        Assertions.assertEquals(resourcePool, unmarshalled);
    }
}
