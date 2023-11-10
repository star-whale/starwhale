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

package ai.starwhale.mlops.domain.ft.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.ft.po.FineTuneSpaceEntity;
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
class FineTuneSpaceMapperTest extends MySqlContainerHolder {

    @Autowired
    FineTuneSpaceMapper fineTuneSpaceMapper;

    FineTuneSpaceEntity ftSpace = FineTuneSpaceEntity.builder()
            .ownerId(4L)
            .projectId(1L)
            .name("spname0")
            .description("spdesc0")
            .build();

    @BeforeEach
    public void setup() {
        add(FineTuneSpaceEntity.builder()
                    .ownerId(3L)
                    .projectId(2L)
                    .name("spname1")
                    .description("spdesc1")
                    .build());
        add(FineTuneSpaceEntity.builder()
                    .ownerId(3L)
                    .projectId(2L)
                    .name("spname2")
                    .description("spdesc2")
                    .build());

        add(ftSpace);
    }

    void add(FineTuneSpaceEntity ftSpace) {
        fineTuneSpaceMapper.add(ftSpace);
    }

    @Test
    void list() {
        List<FineTuneSpaceEntity> list = fineTuneSpaceMapper.list(2L);
        Assertions.assertEquals(2, list.size());
        list = fineTuneSpaceMapper.list(1L);
        Assertions.assertEquals(1, list.size());
        FineTuneSpaceEntity fineTuneSpaceEntity = list.get(0);
        Assertions.assertNotNull(fineTuneSpaceEntity.getId());
        Assertions.assertEquals(4L, fineTuneSpaceEntity.getOwnerId());
        Assertions.assertEquals(1L, fineTuneSpaceEntity.getProjectId());
        Assertions.assertEquals("spname0", fineTuneSpaceEntity.getName());
        Assertions.assertEquals("spdesc0", fineTuneSpaceEntity.getDescription());
    }

    @Test
    void update() {
        fineTuneSpaceMapper.update(ftSpace.getId(), "spnameu", null);
        List<FineTuneSpaceEntity> list = fineTuneSpaceMapper.list(1L);
        Assertions.assertEquals(1, list.size());
        FineTuneSpaceEntity fineTuneSpaceEntity = list.get(0);
        Assertions.assertNotNull(fineTuneSpaceEntity.getId());
        Assertions.assertEquals(4L, fineTuneSpaceEntity.getOwnerId());
        Assertions.assertEquals(1L, fineTuneSpaceEntity.getProjectId());
        Assertions.assertEquals("spnameu", fineTuneSpaceEntity.getName());
        Assertions.assertEquals("spdesc0", fineTuneSpaceEntity.getDescription());

        fineTuneSpaceMapper.update(ftSpace.getId(), null, "spdescu");
        list = fineTuneSpaceMapper.list(1L);
        Assertions.assertEquals(1, list.size());
        fineTuneSpaceEntity = list.get(0);
        Assertions.assertNotNull(fineTuneSpaceEntity.getId());
        Assertions.assertEquals(4L, fineTuneSpaceEntity.getOwnerId());
        Assertions.assertEquals(1L, fineTuneSpaceEntity.getProjectId());
        Assertions.assertEquals("spnameu", fineTuneSpaceEntity.getName());
        Assertions.assertEquals("spdescu", fineTuneSpaceEntity.getDescription());

        fineTuneSpaceMapper.update(ftSpace.getId(), "spdname2", "spdescu2");
        list = fineTuneSpaceMapper.list(1L);
        Assertions.assertEquals(1, list.size());
        fineTuneSpaceEntity = list.get(0);
        Assertions.assertNotNull(fineTuneSpaceEntity.getId());
        Assertions.assertEquals(4L, fineTuneSpaceEntity.getOwnerId());
        Assertions.assertEquals(1L, fineTuneSpaceEntity.getProjectId());
        Assertions.assertEquals("spdname2", fineTuneSpaceEntity.getName());
        Assertions.assertEquals("spdescu2", fineTuneSpaceEntity.getDescription());
    }
}