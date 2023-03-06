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

import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.upload.bo.DatasetVersionWithMeta;
import ai.starwhale.mlops.domain.dataset.upload.bo.Manifest;
import ai.starwhale.mlops.domain.dataset.upload.bo.VersionMeta;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * convert SwDatasetVersionEntity to DatasetVersionWithMeta
 */
@Component
@Slf4j
public class DatasetVersionWithMetaConverter {

    public static final String EMPTY_YAML = "--- {}";

    public DatasetVersionWithMeta from(DatasetVersion entity) {
        VersionMeta versionMeta;
        try {
            Manifest manifest = Constants.yamlMapper.readValue(entity.getVersionMeta(),
                    Manifest.class);
            String filesUploadedStr = entity.getFilesUploaded();
            Map filesUploaded = Constants.yamlMapper.readValue(
                    StringUtils.hasText(filesUploadedStr) ? filesUploadedStr : EMPTY_YAML, Map.class);
            versionMeta = new VersionMeta(manifest, filesUploaded);
        } catch (JsonProcessingException e) {
            log.error("version meta read failed for {}", entity.getId(), e);
            throw new SwValidationException(ValidSubject.DATASET, "version meta read failed");
        }
        if (null == versionMeta.getUploadedFileBlake2bs()) {
            versionMeta.setUploadedFileBlake2bs(new HashMap<>());
        }
        return new DatasetVersionWithMeta(entity, versionMeta);
    }
}
