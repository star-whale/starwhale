/**
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

import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * convert SWDatasetVersionEntity to SWDSVersionWithMeta
 */
@Component
@Slf4j
public class SWDSVersionWithMetaConverter {

    public final static String EMPTY_YAML = "--- {}";

    final ObjectMapper objectMapper;

    public SWDSVersionWithMetaConverter(@Qualifier("yamlMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SWDSVersionWithMeta from(SWDatasetVersionEntity entity){
        VersionMeta versionMeta;
        try {
            Manifest manifest= objectMapper.readValue(entity.getVersionMeta(),
                Manifest.class);
            String filesUploadedStr = entity.getFilesUploaded();
            Map filesUploaded = objectMapper.readValue(StringUtils.hasText(filesUploadedStr)?filesUploadedStr:EMPTY_YAML, Map.class);
            versionMeta = new VersionMeta(manifest,filesUploaded);
        } catch (JsonProcessingException e) {
            log.error("version meta read failed for {}",entity.getId(),e);
            throw new SWValidationException(ValidSubject.SWDS).tip("version meta read failed");
        }
        if(null == versionMeta.getUploadedFileBlake2bs()){
            versionMeta.setUploadedFileBlake2bs(new HashMap<>());
        }
        return new SWDSVersionWithMeta(entity,versionMeta);
    }
}
