/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl.clsmulti.repo;

import ai.starwhale.mlops.resulting.Indicator;
import ai.starwhale.mlops.resulting.repo.IndicatorRepo;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class McIndicatorRepo implements IndicatorRepo {

    final StorageAccessService storageAccessService;

    final ObjectMapper objectMapper;

    protected McIndicatorRepo(StorageAccessService storageAccessService,
        ObjectMapper objectMapper) {
        this.storageAccessService = storageAccessService;
        this.objectMapper = objectMapper;
    }

    final String SPLITER="##SPLITER##";
    @Override
    public Collection<Indicator> loadResult(String resultPath) throws IOException {
        try(InputStream inputStream = storageAccessService.get(resultPath)){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            Collection<Indicator> result = new LinkedList<>();
            while((line = bufferedReader.readLine()) != null) {
                String[] split = line.split(SPLITER);
                try{
                    Indicator indicator = (Indicator)objectMapper.readValue(split[1], Class.forName(split[0]));
                    result.add(indicator);
                }catch (ClassNotFoundException e) {
                    log.error("class not found for {} while collecting result for {}",split[0],
                        resultPath,e);
                }
            }
            return result;
        }

    }
}
