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

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.upgrade.mapper.StarterMapper;
import ai.starwhale.mlops.domain.upgrade.po.StarterEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class StarterMapperTest extends MySqlContainerHolder {

    @Autowired
    private StarterMapper starterMapper;

    @Test
    public void testGetStarter() {
        Assertions.assertEquals(StarterEntity.Starter.SINGLETON, starterMapper.getStarter());
        starterMapper.updateStarter(StarterEntity.Starter.ROLLUP);
        Assertions.assertEquals(StarterEntity.Starter.ROLLUP, starterMapper.getStarter());
        starterMapper.resetStarter();
        Assertions.assertEquals(StarterEntity.Starter.SINGLETON, starterMapper.getStarter());
    }
}
