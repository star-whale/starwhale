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

package ai.starwhale.mlops.domain.swds.upload;

import ai.starwhale.mlops.domain.swds.mapper.SwDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HotSwdsLoader implements CommandLineRunner {

    final SwDatasetVersionMapper swDatasetVersionMapper;

    final HotSwdsHolder hotSwdsHolder;

    public HotSwdsLoader(
            SwDatasetVersionMapper swDatasetVersionMapper,
            HotSwdsHolder hotSwdsHolder) {
        this.swDatasetVersionMapper = swDatasetVersionMapper;
        this.hotSwdsHolder = hotSwdsHolder;
    }

    @Override
    public void run(String... args) throws Exception {
        loadUploadingDs();
    }

    void loadUploadingDs() {
        List<SwDatasetVersionEntity> datasetVersionEntities = swDatasetVersionMapper.findVersionsByStatus(
                SwDatasetVersionEntity.STATUS_UN_AVAILABLE);
        datasetVersionEntities.parallelStream().forEach(entity -> hotSwdsHolder.manifest(entity));
    }
}
