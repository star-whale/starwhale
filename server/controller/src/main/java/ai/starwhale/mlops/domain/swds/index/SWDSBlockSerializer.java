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

import ai.starwhale.mlops.api.protocol.report.resp.SWDSBlockVO;
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

    public List<SWDSBlockVO> fromString(String str) throws JsonProcessingException {
        final String[] lines = str.split(TOKEN_LINE);
        List<SWDSBlockVO> result = new LinkedList<>();
        for(String line:lines){
            result.add(objectMapper.readValue(line,SWDSBlockVO.class));
        }
        return result;
    }

    public String toString(List<SWDSBlockVO> swdsBlocks) throws JsonProcessingException {
        if(null == swdsBlocks || swdsBlocks.size() ==0){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SWDSBlockVO swdsBlock : swdsBlocks) {
            sb.append(objectMapper.writeValueAsString(swdsBlock));
            sb.append(TOKEN_LINE);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

}
