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

import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DuplicateKeyException;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class DatasetMapperTest extends MySqlContainerHolder {

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

    @Test
    public void testList() {
        DatasetEntity dataset1 = DatasetEntity.builder()
                .datasetName("dataset1")
                .ownerId(1L)
                .projectId(1L)
                .isDeleted(0)
                .build();
        DatasetEntity dataset2 = DatasetEntity.builder()
                .datasetName("dataset2")
                .ownerId(2L)
                .projectId(2L)
                .isDeleted(0)
                .build();
        DatasetEntity dataset3 = DatasetEntity.builder()
                .datasetName("dataset3")
                .ownerId(2L)
                .projectId(1L)
                .isDeleted(0)
                .build();

        datasetMapper.insert(dataset1);
        datasetMapper.insert(dataset2);
        datasetMapper.insert(dataset3);

        var list = datasetMapper.list(1L, null, null, null);
        Assertions.assertIterableEquals(List.of(dataset3, dataset1), list);

        list = datasetMapper.list(2L, null, null, null);
        Assertions.assertIterableEquals(List.of(dataset2), list);

        list = datasetMapper.list(null, "dataset", null, null);
        Assertions.assertIterableEquals(List.of(dataset3, dataset2, dataset1), list);

        list = datasetMapper.list(null, "dataset1", null, null);
        Assertions.assertIterableEquals(List.of(dataset1), list);

        list = datasetMapper.list(null, null, 2L, null);
        Assertions.assertIterableEquals(List.of(dataset3, dataset2), list);
    }

    @Test
    public void testRemoveAndRecover() {
        DatasetEntity dataset1 = DatasetEntity.builder()
                .datasetName("dataset1")
                .ownerId(1L)
                .projectId(1L)
                .isDeleted(0)
                .build();
        datasetMapper.insert(dataset1);

        DatasetEntity duplicate = DatasetEntity.builder()
                .datasetName("dataset1")
                .ownerId(1L)
                .projectId(1L)
                .build();
        assertThrows(DuplicateKeyException.class, () -> datasetMapper.insert(duplicate));

        datasetMapper.remove(dataset1.getId());
        Assertions.assertEquals(1, datasetMapper.find(dataset1.getId()).getIsDeleted());
        Assertions.assertNull(datasetMapper.findByName(dataset1.getDatasetName(), dataset1.getProjectId(), false));
        Assertions.assertEquals(dataset1.getDatasetName(),
                datasetMapper.findDeleted(dataset1.getId()).getDatasetName());

        datasetMapper.insert(duplicate);
        assertThrows(DuplicateKeyException.class, () -> datasetMapper.recover(dataset1.getId()));
        datasetMapper.remove(duplicate.getId());

        datasetMapper.recover(dataset1.getId());
        Assertions.assertEquals(0, datasetMapper.find(dataset1.getId()).getIsDeleted());
        Assertions.assertEquals(dataset1,
                datasetMapper.findByName(dataset1.getDatasetName(), dataset1.getProjectId(), false));
        Assertions.assertNull(datasetMapper.findDeleted(dataset1.getId()));

        assertThrows(DuplicateKeyException.class, () -> datasetMapper.insert(duplicate));
    }
}
