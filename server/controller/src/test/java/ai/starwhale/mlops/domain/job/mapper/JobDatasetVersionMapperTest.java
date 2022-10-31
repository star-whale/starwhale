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


    @Autowired
    private JobDatasetVersionMapper jobDatasetVersionMapper;

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private DatasetVersionMapper datasetVersionMapper;

    DatasetEntity dataset;
    DatasetVersionEntity datasetVersionEntity;
    DatasetVersionEntity datasetVersionEntity2;

    @BeforeEach
    public void initData() {
        dataset = DatasetEntity.builder().datasetName("dsn").projectId(1L).ownerId(1L).build();
        datasetMapper.addDataset(dataset);

        datasetVersionEntity = DatasetVersionEntity.builder().datasetId(dataset.getId()).versionName("vn")
                .filesUploaded("fl").storageAuths("sta").versionTag("vt").versionMeta("vm").indexTable("idt").size(123L)
                .storagePath("stp").ownerId(1L).build();
        datasetVersionEntity2 = DatasetVersionEntity.builder().datasetId(dataset.getId()).versionName("vn2")
                .filesUploaded("fl2").storageAuths("sta2").versionTag("vt2").versionMeta("vm2").indexTable("idt2")
                .size(1223L)
                .storagePath("stp2").ownerId(1L).build();
        datasetVersionMapper.addNewVersion(datasetVersionEntity);
        datasetVersionMapper.addNewVersion(datasetVersionEntity2);
        jobDatasetVersionMapper.addJobDatasetVersions(13L,
                List.of(datasetVersionEntity.getId(), datasetVersionEntity2.getId()));
    }

    @Test
    public void testListSwdsVersionsByJobId() {
        List<DatasetVersionEntity> swDatasetVersionEntities = jobDatasetVersionMapper.listDatasetVersionsByJobId(
                13L);
        Assertions.assertEquals(2, swDatasetVersionEntities.size());
        swDatasetVersionEntities.forEach(entity -> assertDsEquals(
                entity.getId().equals(datasetVersionEntity.getId()) ? datasetVersionEntity
                        : datasetVersionEntity2, entity));

    }


    public void assertDsEquals(DatasetVersionEntity expected, DatasetVersionEntity acutal) {
        Assertions.assertEquals(expected.getId(), acutal.getId());
        Assertions.assertEquals(expected.getDatasetId(), acutal.getDatasetId());
        Assertions.assertEquals(expected.getVersionOrder(), acutal.getVersionOrder());
        Assertions.assertEquals("dsn", acutal.getDatasetName());
        Assertions.assertEquals(expected.getOwnerId(), acutal.getOwnerId());
        Assertions.assertEquals(expected.getOwner(), acutal.getOwner());
        Assertions.assertEquals(expected.getVersionName(), acutal.getVersionName());
        Assertions.assertEquals(expected.getVersionTag(), acutal.getVersionTag());
        Assertions.assertEquals(expected.getVersionMeta(), acutal.getVersionMeta());
        Assertions.assertEquals(expected.getFilesUploaded(), acutal.getFilesUploaded());
        Assertions.assertEquals(expected.getStoragePath(), acutal.getStoragePath());
        Assertions.assertEquals(expected.getSize(), acutal.getSize());
        Assertions.assertEquals(expected.getStorageAuths(), acutal.getStorageAuths());
        Assertions.assertEquals(expected.getIndexTable(), acutal.getIndexTable());
        Assertions.assertEquals(0, acutal.getStatus());
    }
}
