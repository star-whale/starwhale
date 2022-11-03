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

package ai.starwhale.mlops.domain.trash.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.trash.po.TrashPo;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class TrashMapperTest extends MySqlContainerHolder {

    @Autowired
    private TrashMapper trashMapper;

    @Test
    public void testInsertFindAndDelete() {
        TrashPo trashPo = TrashPo.builder()
                .projectId(1L)
                .objectId(1L)
                .operatorId(1L)
                .size(10L)
                .trashName("model1")
                .trashType("MODEL")
                .retention(new Date())
                .updatedTime(new Date())
                .build();
        trashMapper.insert(trashPo);
        Assertions.assertEquals(trashPo, trashMapper.find(trashPo.getId()));

        trashMapper.delete(trashPo.getId());
        Assertions.assertNull(trashMapper.find(trashPo.getId()));
    }

    @Test
    public void testList() {
        TrashPo trash1 = TrashPo.builder()
                .projectId(1L)
                .objectId(1L)
                .operatorId(1L)
                .trashName("model1")
                .trashType("MODEL")
                .retention(new Date())
                .updatedTime(new Date())
                .build();
        TrashPo trash2 = TrashPo.builder()
                .projectId(1L)
                .objectId(1L)
                .operatorId(1L)
                .trashName("dataset1")
                .trashType("DATASET")
                .retention(new Date())
                .updatedTime(new Date())
                .build();
        TrashPo trash3 = TrashPo.builder()
                .projectId(1L)
                .objectId(1L)
                .operatorId(2L)
                .trashName("runtime1")
                .trashType("RUNTIME")
                .retention(new Date())
                .updatedTime(new Date())
                .build();
        TrashPo trash4 = TrashPo.builder()
                .projectId(1L)
                .objectId(1L)
                .operatorId(2L)
                .trashName("job1")
                .trashType("EVALUATION")
                .retention(new Date())
                .updatedTime(new Date())
                .build();
        trashMapper.insert(trash1);
        trashMapper.insert(trash2);
        trashMapper.insert(trash3);
        trashMapper.insert(trash4);

        List<TrashPo> list = trashMapper.list(1L, null, null, null);
        Assertions.assertIterableEquals(List.of(trash4, trash3, trash2, trash1), list);

        list = trashMapper.list(1L, 2L, null, null);
        Assertions.assertIterableEquals(List.of(trash4, trash3), list);

        list = trashMapper.list(1L, null, "model", null);
        Assertions.assertIterableEquals(List.of(trash1), list);

        list = trashMapper.list(1L, null, null, "DATASET");
        Assertions.assertIterableEquals(List.of(trash2), list);
    }
}
