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


import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatasetVersionMapperTest extends MySqlContainerHolder {
    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private DatasetVersionMapper datasetVersionMapper;


    @Test
    public void testUnShareByProject() {
        var dataset = DatasetEntity.builder()
                .projectId(1L)
                .ownerId(1L)
                .datasetName("dataset1")
                .build();
        datasetMapper.insert(dataset);
        var datasetVersion = DatasetVersionEntity.builder()
                .datasetId(dataset.getId())
                .versionName("v1")
                .ownerId(1L)
                .size(3L)
                .indexTable("table1")
                .versionMeta("")
                .storagePath("")
                .shared(true)
                .build();
        datasetVersionMapper.insert(datasetVersion);

        var dataset2 = DatasetEntity.builder()
                .projectId(2L)
                .ownerId(1L)
                .datasetName("dataset2")
                .build();
        datasetMapper.insert(dataset2);
        var datasetVersion2 = DatasetVersionEntity.builder()
                .datasetId(dataset2.getId())
                .versionName("v2")
                .ownerId(1L)
                .size(3L)
                .indexTable("table2")
                .versionMeta("")
                .storagePath("")
                .shared(true)
                .build();
        datasetVersionMapper.insert(datasetVersion2);

        datasetVersionMapper.unShareDatesetVersionWithinProject(dataset.getProjectId());
        var dv = datasetVersionMapper.find(datasetVersion.getId());
        assertEquals(false, dv.getShared());
        var dv2 = datasetVersionMapper.find(datasetVersion2.getId());
        assertEquals(true, dv2.getShared());
    }
}
