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

package ai.starwhale.mlops.domain.swds.index;

import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Primary
@Service
public class SWDSIndexLoaderImpl implements SWDSIndexLoader {

    final StorageAccessService storageAccessService;

    final ObjectMapper objectMapper;

    public SWDSIndexLoaderImpl(StorageAccessService storageAccessService, ObjectMapper objectMapper) {
        this.storageAccessService = storageAccessService;
        this.objectMapper = objectMapper;
    }

    @Override
    public SWDSIndex load(String storagePath) {

        try(final InputStream inputStream = storageAccessService.get(storagePath)){
            List<SWDSBlock> lines = new LinkedList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (reader.ready()) {
                SWDSBlock swdsBlock = objectMapper.readValue(reader.readLine(), SWDSBlock.class);
                lines.add(swdsBlock);
            }
            return SWDSIndex.builder().storagePath(storagePath).swdsBlockList(lines).build();
        } catch (IOException e) {
            log.error("error while reading index file {}",storagePath,e);
            throw new SWValidationException(ValidSubject.SWDS);
        }

    }

}
