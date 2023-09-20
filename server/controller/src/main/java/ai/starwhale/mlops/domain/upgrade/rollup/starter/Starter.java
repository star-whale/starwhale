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

package ai.starwhale.mlops.domain.upgrade.rollup.starter;

import ai.starwhale.mlops.domain.upgrade.mapper.StarterMapper;
import ai.starwhale.mlops.domain.upgrade.mapper.TableCheckMapper;
import ai.starwhale.mlops.domain.upgrade.po.StarterEntity;
import org.springframework.stereotype.Component;

@Component
public class Starter {
    final TableCheckMapper tableCheckMapper;
    final StarterMapper starterMapper;

    public Starter(TableCheckMapper tableCheckMapper, StarterMapper starterMapper) {
        this.tableCheckMapper = tableCheckMapper;
        this.starterMapper = starterMapper;
    }

    public boolean rollupStart() {
        if (tableCheckMapper.checkTable("starter") == 0) {
            return false;
        }
        StarterEntity.Starter starter = starterMapper.getStarter();
        if (StarterEntity.Starter.ROLLUP == starter) {
            return true;
        }
        return false;
    }

    public void reset() {
        if (tableCheckMapper.checkTable("starter") == 0) {
            return;
        }
        starterMapper.resetStarter();
    }
}
