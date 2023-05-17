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

package ai.starwhale.mlops.domain.job.mapper;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class JobDatasetVersionMapperTest extends MySqlContainerHolder {


    DatasetEntity dataset;
    DatasetVersionEntity datasetVersionEntity;
    DatasetVersionEntity datasetVersionEntity2;
    @Autowired
    private JobDatasetVersionMapper jobDatasetVersionMapper;
    @Autowired
    private DatasetMapper datasetMapper;
    @Autowired
    private DatasetVersionMapper datasetVersionMapper;

    @BeforeEach
    public void initData() {
        dataset = DatasetEntity.builder().datasetName("dsn").projectId(1L).ownerId(1L).build();
        datasetMapper.insert(dataset);

        datasetVersionEntity = DatasetVersionEntity.builder().datasetId(dataset.getId()).versionName("vn")
                .filesUploaded("fl").versionTag("vt").versionMeta("vm").indexTable("idt").size(123L)
                .storagePath("stp").ownerId(1L).build();
        datasetVersionEntity2 = DatasetVersionEntity.builder().datasetId(dataset.getId()).versionName("vn2")
                .filesUploaded("fl2").versionTag("vt2").versionMeta("vm2").indexTable("idt2")
                .size(1223L)
                .storagePath("stp2").ownerId(1L).build();
        datasetVersionMapper.insert(datasetVersionEntity);
        datasetVersionMapper.insert(datasetVersionEntity2);
        jobDatasetVersionMapper.insert(13L,
                Set.of(datasetVersionEntity.getId(), datasetVersionEntity2.getId()));
    }

    @Test
    public void testListSwdsVersionsByJobId() {
        List<Long> ids = jobDatasetVersionMapper.listDatasetVersionIdsByJobId(
                13L);
        Assertions.assertEquals(2, ids.size());
        Assertions.assertIterableEquals(
                List.of(datasetVersionEntity.getId(), datasetVersionEntity2.getId()),
                ids
        );

    }
}