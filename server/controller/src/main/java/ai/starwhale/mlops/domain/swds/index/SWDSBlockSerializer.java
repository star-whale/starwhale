/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * serialize & deserialize SWDSBlocks
 */
@Component
public class SWDSBlockSerializer {

    private static final String TOKEN_LINE="\n";

    final ObjectMapper objectMapper;

    public SWDSBlockSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<SWDSBlock> fromString(String str) throws JsonProcessingException {
        final String[] lines = str.split(TOKEN_LINE);
        List<SWDSBlock> result = new LinkedList<>();
        for(String line:lines){
            result.add(objectMapper.readValue(line,SWDSBlock.class));
        }
        return result;
    }

    public String toString(List<SWDSBlock> swdsBlocks) throws JsonProcessingException {
        if(null == swdsBlocks || swdsBlocks.size() ==0){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SWDSBlock swdsBlock : swdsBlocks) {
            sb.append(objectMapper.writeValueAsString(swdsBlock));
            sb.append(TOKEN_LINE);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

}
