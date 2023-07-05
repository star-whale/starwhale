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

package ai.starwhale.mlops.domain.runtime.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.runtime.RuntimeTestConstants;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class RuntimeVersionMapperTest extends MySqlContainerHolder {
    @Autowired
    private RuntimeVersionMapper runtimeVersionMapper;
    @Autowired
    private RuntimeMapper runtimeMapper;

    @Test
    public void testFindLatestByProjectId() throws InterruptedException {
        RuntimeEntity runtimeEntity = RuntimeEntity.builder()
                .runtimeName("runtime")
                .ownerId(1L)
                .projectId(2L)
                .build();
        runtimeMapper.insert(runtimeEntity);

        var runtimeVersionEntity1 = RuntimeVersionEntity.builder()
                .runtimeId(runtimeEntity.getId())
                .ownerId(1L)
                .versionName("version 1")
                .versionMeta(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)
                .storagePath("storage path")
                .build();
        runtimeVersionMapper.insert(runtimeVersionEntity1);
        runtimeVersionEntity1.setVersionOrder(0L);

        var runtimeVersionEntity2 = RuntimeVersionEntity.builder()
                .runtimeId(runtimeEntity.getId())
                .ownerId(1L)
                .versionName("version 2")
                .versionMeta(RuntimeTestConstants.MANIFEST_WITHOUT_BUILTIN_IMAGE)
                .storagePath("storage path")
                .build();
        // make sure modify time changes
        Thread.sleep(1000);
        runtimeVersionMapper.insert(runtimeVersionEntity2);
        runtimeVersionEntity2.setVersionOrder(0L);

        var rt = runtimeVersionMapper.findLatestByProjectId(2L, null);
        assertEquals(2, rt.size());
        assertEquals("version 2", rt.get(0).getVersionName());
        assertEquals(RuntimeTestConstants.CUSTOM_IMAGE, rt.get(0).getImage());
        assertEquals("version 1", rt.get(1).getVersionName());
        assertEquals(RuntimeTestConstants.BUILTIN_IMAGE, rt.get(1).getImage());

        rt = runtimeVersionMapper.findLatestByProjectId(2L, 1);
        assertEquals(1, rt.size());
        assertEquals("version 2", rt.get(0).getVersionName());
        assertEquals(RuntimeTestConstants.CUSTOM_IMAGE, rt.get(0).getImage());

        rt = runtimeVersionMapper.findLatestByProjectId(3L, null);
        assertEquals(List.of(), rt);
    }

    @Test
    public void testUnShareByProject() {
        var runtime = RuntimeEntity.builder()
                .runtimeName("runtime")
                .ownerId(1L)
                .projectId(2L)
                .build();
        runtimeMapper.insert(runtime);
        var runtimeVersion = RuntimeVersionEntity.builder()
                .runtimeId(runtime.getId())
                .ownerId(1L)
                .versionName("version 1")
                .versionMeta(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)
                .storagePath("storage path")
                .shared(true)
                .build();
        runtimeVersionMapper.insert(runtimeVersion);

        // add runtime version that do not belong to the project
        var runtime2 = RuntimeEntity.builder()
                .runtimeName("runtime2")
                .ownerId(1L)
                .projectId(3L)
                .build();
        runtimeMapper.insert(runtime2);
        var runtimeVersion2 = RuntimeVersionEntity.builder()
                .runtimeId(runtime2.getId())
                .ownerId(1L)
                .versionName("version 2")
                .versionMeta(RuntimeTestConstants.MANIFEST_WITH_BUILTIN_IMAGE)
                .storagePath("storage path")
                .shared(true)
                .build();
        runtimeVersionMapper.insert(runtimeVersion2);

        runtimeVersionMapper.unShareRuntimeVersionWithinProject(2L);

        var rv = runtimeVersionMapper.find(runtimeVersion.getId());
        assertEquals(false, rv.getShared());

        // the shared attribute of runtime version that do not belong to the project should not be changed
        var rv2 = runtimeVersionMapper.find(runtimeVersion2.getId());
        assertEquals(true, rv2.getShared());
    }
}
