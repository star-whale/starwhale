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

package ai.starwhale.mlops.domain.dataset.upload;

import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HotDatasetLoader implements CommandLineRunner {

    final DatasetVersionMapper datasetVersionMapper;

    final HotDatasetHolder hotDatasetHolder;

    public HotDatasetLoader(
            DatasetVersionMapper datasetVersionMapper,
            HotDatasetHolder hotDatasetHolder) {
        this.datasetVersionMapper = datasetVersionMapper;
        this.hotDatasetHolder = hotDatasetHolder;
    }

    @Override
    public void run(String... args) throws Exception {
        loadUploadingDs();
    }

    void loadUploadingDs() {
        List<DatasetVersionEntity> datasetVersionEntities = datasetVersionMapper.findByStatus(
                DatasetVersionEntity.STATUS_UN_AVAILABLE);
        datasetVersionEntities.parallelStream().forEach(entity -> hotDatasetHolder.manifest(entity));
    }
}
