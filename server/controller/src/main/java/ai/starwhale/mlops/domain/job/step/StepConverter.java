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

import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class StepConverter {

    public StepConverter() {
    }

    public Step fromEntity(StepEntity entity) throws IOException {
        log.debug("from step entity");
        ResourcePool pool = null;
        if (StringUtils.hasText(entity.getPoolInfo())) {
            pool = ResourcePool.fromJson(entity.getPoolInfo());
        }
        Step step = Step.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .name(entity.getName())
                .resourcePool(pool)
                .build();
        step.setStartTime(entity.getStartedTime().getTime());
        step.setFinishTime(entity.getFinishedTime().getTime());
        return step;
    }

}
