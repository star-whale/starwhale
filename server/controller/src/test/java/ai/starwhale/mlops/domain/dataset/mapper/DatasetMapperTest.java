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

package ai.starwhale.mlops.domain.dataset.mapper;

import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class DatasetMapperTest {

    @Autowired
    private DatasetMapper datasetMapper;

    @Test
    public void testInsertAndFind() {
        DatasetEntity entity = DatasetEntity.builder()
                .datasetName("model1")
                .ownerId(1L)
                .projectId(2L)
                .build();
        datasetMapper.insert(entity);
        entity.setIsDeleted(0);

        Assertions.assertEquals(entity, datasetMapper.find(entity.getId()));
        Assertions.assertEquals(entity, datasetMapper.findByName(entity.getDatasetName(),
                entity.getProjectId(), false));
        Assertions.assertEquals(entity, datasetMapper.findByName(entity.getDatasetName(),
                entity.getProjectId(), true));
    }
}
