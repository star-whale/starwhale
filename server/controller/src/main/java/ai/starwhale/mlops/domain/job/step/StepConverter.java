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

package ai.starwhale.mlops.domain.job.step;

import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StepConverter {

    final LocalDateTimeConvertor localDateTimeConvertor;

    public StepConverter(LocalDateTimeConvertor localDateTimeConvertor) {
        this.localDateTimeConvertor = localDateTimeConvertor;
    }

    public Step fromEntity(StepEntity entity) {
        log.debug("from step entity");
        Step step = Step.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .name(entity.getName())
                .build();
        step.setStartTime(localDateTimeConvertor.convert(entity.getStartedTime()));
        step.setFinishTime(localDateTimeConvertor.convert(entity.getFinishedTime()));
        return step;
    }

}
