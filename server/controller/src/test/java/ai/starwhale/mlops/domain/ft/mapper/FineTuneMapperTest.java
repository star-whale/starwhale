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
import ai.starwhale.mlops.domain.ft.po.FineTuneEntity;
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
class FineTuneMapperTest extends MySqlContainerHolder {

    @Autowired
    FineTuneMapper fineTuneMapper;

    FineTuneEntity ft = FineTuneEntity.builder()
            .spaceId(11L)
            .jobId(12L)
            .validationDatasets(List.of(1L, 2L, 3L))
            .trainDatasets(List.of(4L, 5L))
            .baseModelVersionId(13L)
            .targetModelVersionId(null)
            .build();

    @BeforeEach
    public void setup() {

        add(ft);
        Assertions.assertNotNull(ft.getId());

        add(
                FineTuneEntity.builder()
                        .spaceId(11L)
                        .jobId(13L)
                        .validationDatasets(List.of(12L, 22L, 32L))
                        .trainDatasets(List.of(42L, 52L))
                        .baseModelVersionId(23L)
                        .targetModelVersionId(null)
                        .build()
        );

        add(
                FineTuneEntity.builder()
                        .spaceId(12L)
                        .jobId(14L)
                        .validationDatasets(List.of(13L, 23L, 33L))
                        .trainDatasets(List.of(43L, 53L))
                        .baseModelVersionId(33L)
                        .targetModelVersionId(3L)
                        .build()
        );

    }

    void add(FineTuneEntity ft) {
        fineTuneMapper.add(ft);
    }

    @Test
    void list() {
        List<FineTuneEntity> list = fineTuneMapper.list(11L);
        Assertions.assertEquals(2L, list.size());
        list = fineTuneMapper.list(12L);
        Assertions.assertEquals(1L, list.size());
        FineTuneEntity fineTuneEntity = list.get(0);
        Assertions.assertNotNull(fineTuneEntity.getId());
        Assertions.assertEquals(12L, fineTuneEntity.getSpaceId());
        Assertions.assertEquals(14L, fineTuneEntity.getJobId());
        Assertions.assertIterableEquals(List.of(13L, 23L, 33L), fineTuneEntity.getValidationDatasets());
        Assertions.assertIterableEquals(List.of(43L, 53L), fineTuneEntity.getTrainDatasets());
        Assertions.assertEquals(33L, fineTuneEntity.getBaseModelVersionId());
        Assertions.assertEquals(3L, fineTuneEntity.getTargetModelVersionId());
    }

    @Test
    void findFtByJob() {
        FineTuneEntity fineTuneEntity = fineTuneMapper.findByJob(14L);
        Assertions.assertNotNull(fineTuneEntity.getId());
        Assertions.assertEquals(12L, fineTuneEntity.getSpaceId());
        Assertions.assertEquals(14L, fineTuneEntity.getJobId());
        Assertions.assertIterableEquals(List.of(13L, 23L, 33L), fineTuneEntity.getValidationDatasets());
        Assertions.assertIterableEquals(List.of(43L, 53L), fineTuneEntity.getTrainDatasets());
        Assertions.assertEquals(33L, fineTuneEntity.getBaseModelVersionId());
        Assertions.assertEquals(3L, fineTuneEntity.getTargetModelVersionId());
    }

    @Test
    void testModelId() {
        fineTuneMapper.updateTargetModel(ft.getId(), 555L);
        FineTuneEntity ft = fineTuneMapper.findByJob(12L);
        Assertions.assertEquals(555L, ft.getTargetModelVersionId());
    }
}