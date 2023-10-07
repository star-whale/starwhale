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

package ai.starwhale.mlops.domain.run;

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.mapper.RunMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class RunDao {

    final RunMapper runMapper;

    final ObjectMapper objectMapper;

    public RunDao(RunMapper runMapper, ObjectMapper objectMapper) {
        this.runMapper = runMapper;
        this.objectMapper = objectMapper;
    }

    public Run findById(Long id) {
        RunEntity runEntity = runMapper.get(id);
        return convertEntityToBo(runEntity);
    }

    public Run convertEntityToBo(RunEntity runEntity) {
        Run run = Run.builder()
                .id(runEntity.getId())
                .status(runEntity.getStatus())
                .runSpec(runEntity.getRunSpec())
                .logDir(runEntity.getLogDir())
                .finishTime(timestampOf(runEntity.getFinishTime()))
                .taskId(runEntity.getTaskId())
                .ip(runEntity.getIp())
                .startTime(timestampOf(runEntity.getStartTime()))
                .failedReason(runEntity.getFailedReason())
                .build();
        return run;
    }

    private Long timestampOf(Date date) {
        if (null == date) {
            return null;
        }
        return date.getTime();
    }
}
