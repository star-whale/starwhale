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

package ai.starwhale.mlops.domain.sft.mapper;

import ai.starwhale.mlops.domain.sft.po.SftSpaceEntity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class SftSpaceMapperTest {

    @Autowired
    SftSpaceMapper sftSpaceMapper;

    SftSpaceEntity sftSpace = SftSpaceEntity.builder()
            .ownerId(4L)
            .projectId(1L)
            .name("spname0")
            .description("spdesc0")
            .build();

    @BeforeEach
    public void setup() {
        add(SftSpaceEntity.builder()
                    .ownerId(3L)
                    .projectId(2L)
                    .name("spname1")
                    .description("spdesc1")
                    .build());
        add(SftSpaceEntity.builder()
                    .ownerId(3L)
                    .projectId(2L)
                    .name("spname2")
                    .description("spdesc2")
                    .build());

        add(sftSpace);
    }

    void add(SftSpaceEntity sftSpace) {
        sftSpaceMapper.add(sftSpace);
    }

    @Test
    void list() {
        List<SftSpaceEntity> list = sftSpaceMapper.list(2L);
        Assertions.assertEquals(2, list.size());
        list = sftSpaceMapper.list(1L);
        Assertions.assertEquals(1, list.size());
        SftSpaceEntity sftSpaceEntity = list.get(0);
        Assertions.assertNotNull(sftSpaceEntity.getId());
        Assertions.assertEquals(4L, sftSpaceEntity.getOwnerId());
        Assertions.assertEquals(1L, sftSpaceEntity.getProjectId());
        Assertions.assertEquals("spname0", sftSpaceEntity.getName());
        Assertions.assertEquals("spdesc0", sftSpaceEntity.getDescription());
    }

    @Test
    void update() {
        sftSpaceMapper.update(sftSpace.getId(), "spnameu", null);
        List<SftSpaceEntity> list = sftSpaceMapper.list(1L);
        Assertions.assertEquals(1, list.size());
        SftSpaceEntity sftSpaceEntity = list.get(0);
        Assertions.assertNotNull(sftSpaceEntity.getId());
        Assertions.assertEquals(4L, sftSpaceEntity.getOwnerId());
        Assertions.assertEquals(1L, sftSpaceEntity.getProjectId());
        Assertions.assertEquals("spnameu", sftSpaceEntity.getName());
        Assertions.assertEquals("spdesc0", sftSpaceEntity.getDescription());

        sftSpaceMapper.update(sftSpace.getId(), null, "spdescu");
        list = sftSpaceMapper.list(1L);
        Assertions.assertEquals(1, list.size());
        sftSpaceEntity = list.get(0);
        Assertions.assertNotNull(sftSpaceEntity.getId());
        Assertions.assertEquals(4L, sftSpaceEntity.getOwnerId());
        Assertions.assertEquals(1L, sftSpaceEntity.getProjectId());
        Assertions.assertEquals("spnameu", sftSpaceEntity.getName());
        Assertions.assertEquals("spdescu", sftSpaceEntity.getDescription());

        sftSpaceMapper.update(sftSpace.getId(), "spdname2", "spdescu2");
        list = sftSpaceMapper.list(1L);
        Assertions.assertEquals(1, list.size());
        sftSpaceEntity = list.get(0);
        Assertions.assertNotNull(sftSpaceEntity.getId());
        Assertions.assertEquals(4L, sftSpaceEntity.getOwnerId());
        Assertions.assertEquals(1L, sftSpaceEntity.getProjectId());
        Assertions.assertEquals("spdname2", sftSpaceEntity.getName());
        Assertions.assertEquals("spdescu2", sftSpaceEntity.getDescription());
    }
}