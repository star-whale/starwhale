/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.bo;

import ai.starwhale.mlops.api.protocol.report.resp.ResultPath;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ResultPathConverter {

    final ObjectMapper objectMapper;

    public ResultPathConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public String toString(ResultPath resultPath) throws JsonProcessingException {
        return objectMapper.writeValueAsString(resultPath);
    }

    public ResultPath fromString(String s) throws JsonProcessingException {
        return objectMapper.readValue(s, ResultPath.class);
    }

}
