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

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.runtime.RuntimeTestConstants;
import ai.starwhale.mlops.domain.runtime.po.RuntimeEntity;
import ai.starwhale.mlops.domain.runtime.po.RuntimeVersionEntity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
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
                .image("image")
                .build();
        runtimeVersionMapper.insert(runtimeVersionEntity1);
        runtimeVersionEntity1.setVersionOrder(0L);

        var runtimeVersionEntity2 = RuntimeVersionEntity.builder()
                .runtimeId(runtimeEntity.getId())
                .ownerId(1L)
                .versionName("version 2")
                .versionMeta(RuntimeTestConstants.MANIFEST_WITHOUT_BUILTIN_IMAGE)
                .storagePath("storage path")
                .image("image")
                .build();
        // make sure modify time changes
        Thread.sleep(1000);
        runtimeVersionMapper.insert(runtimeVersionEntity2);
        runtimeVersionEntity2.setVersionOrder(0L);

        var rt = runtimeVersionMapper.findLatestByProjectId(2L, null);
        Assertions.assertEquals(2, rt.size());
        Assertions.assertEquals("version 2", rt.get(0).getVersionName());
        Assertions.assertEquals(RuntimeTestConstants.CUSTOM_IMAGE, rt.get(0).getImage());
        Assertions.assertEquals("version 1", rt.get(1).getVersionName());
        Assertions.assertEquals(RuntimeTestConstants.BUILTIN_IMAGE, rt.get(1).getImage());

        rt = runtimeVersionMapper.findLatestByProjectId(2L, 1);
        Assertions.assertEquals(1, rt.size());
        Assertions.assertEquals("version 2", rt.get(0).getVersionName());
        Assertions.assertEquals(RuntimeTestConstants.CUSTOM_IMAGE, rt.get(0).getImage());

        rt = runtimeVersionMapper.findLatestByProjectId(3L, null);
        Assertions.assertEquals(List.of(), rt);
    }
}
