/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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

@Slf4j
public class SWDSIndexLoaderImpl implements SWDSIndexLoader {

    StorageAccessService storageAccessService;

    ObjectMapper objectMapper;

    @Override
    public SWDSIndex load(String storagePath) {

        try(final InputStream inputStream = storageAccessService.get(storagePath)){
            List<SWDSBlock> lines = new LinkedList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (reader.ready()) {
                lines.add(objectMapper.readValue(reader.readLine(),SWDSBlock.class));
            }
            return SWDSIndex.builder().storagePath(storagePath).SWDSBlockList(lines).build();
        } catch (IOException e) {
            log.error("error while reading index file {}",storagePath,e);
            throw new SWValidationException(ValidSubject.SWDS);
        }

    }

}
