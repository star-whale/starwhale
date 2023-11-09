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

import ai.starwhale.mlops.domain.sft.po.SftEntity;
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
class SftMapperTest {

    @Autowired
    SftMapper sftMapper;

    SftEntity sft = SftEntity.builder()
            .spaceId(11L)
            .jobId(12L)
            .evalDatasets(List.of(1L, 2L, 3L))
            .trainDatasets(List.of(4L, 5L))
            .baseModelVersionId(13L)
            .targetModelVersionId(null)
            .build();

    @BeforeEach
    public void setup() {

        add(sft);
        Assertions.assertNotNull(sft.getId());

        add(
                SftEntity.builder()
                        .spaceId(11L)
                        .jobId(13L)
                        .evalDatasets(List.of(12L, 22L, 32L))
                        .trainDatasets(List.of(42L, 52L))
                        .baseModelVersionId(23L)
                        .targetModelVersionId(null)
                        .build()
        );

        add(
                SftEntity.builder()
                        .spaceId(12L)
                        .jobId(14L)
                        .evalDatasets(List.of(13L, 23L, 33L))
                        .trainDatasets(List.of(43L, 53L))
                        .baseModelVersionId(33L)
                        .targetModelVersionId(3L)
                        .build()
        );

    }

    void add(SftEntity sft) {
        sftMapper.add(sft);
    }

    @Test
    void list() {
        List<SftEntity> list = sftMapper.list(11L);
        Assertions.assertEquals(2L, list.size());
        list = sftMapper.list(12L);
        Assertions.assertEquals(1L, list.size());
        SftEntity sftEntity = list.get(0);
        Assertions.assertNotNull(sftEntity.getId());
        Assertions.assertEquals(12L, sftEntity.getSpaceId());
        Assertions.assertEquals(14L, sftEntity.getJobId());
        Assertions.assertIterableEquals(List.of(13L, 23L, 33L), sftEntity.getEvalDatasets());
        Assertions.assertIterableEquals(List.of(43L, 53L), sftEntity.getTrainDatasets());
        Assertions.assertEquals(33L, sftEntity.getBaseModelVersionId());
        Assertions.assertEquals(3L, sftEntity.getTargetModelVersionId());
    }

    @Test
    void findSftByJob() {
        SftEntity sftEntity = sftMapper.findSftByJob(14L);
        Assertions.assertNotNull(sftEntity.getId());
        Assertions.assertEquals(12L, sftEntity.getSpaceId());
        Assertions.assertEquals(14L, sftEntity.getJobId());
        Assertions.assertIterableEquals(List.of(13L, 23L, 33L), sftEntity.getEvalDatasets());
        Assertions.assertIterableEquals(List.of(43L, 53L), sftEntity.getTrainDatasets());
        Assertions.assertEquals(33L, sftEntity.getBaseModelVersionId());
        Assertions.assertEquals(3L, sftEntity.getTargetModelVersionId());
    }

    @Test
    void testModelId() {
        sftMapper.updateTargetModel(sft.getId(), 555L);
        SftEntity sftByJob = sftMapper.findSftByJob(12L);
        Assertions.assertEquals(555L, sftByJob.getTargetModelVersionId());
    }
}