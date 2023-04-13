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

package ai.starwhale.mlops.domain.settings;

import ai.starwhale.mlops.domain.MySqlContainerHolder;
import ai.starwhale.mlops.domain.settings.mapper.SettingsMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SettingsMapperTest extends MySqlContainerHolder {

    @Autowired
    private SettingsMapper mapper;

    String settings = "env:\n"
            + "  SW_ENV_1: 'sw_env_1'\n"
            + "  SW_ENV_2: 'sw_env_2'\n"
            + "  SW_ENV_3: 'sw_env_3'\n"
            + "others:\n"
            + "- OTH: ''";

    String invalidSettings = "123";

    @Test
    public void testInsertAndQuery() {
        Assertions.assertEquals(mapper.insert(1L, settings, Scope.USER), 1);

        var userSettings = mapper.get(Scope.USER, 1L);
        Assertions.assertNotNull(userSettings);
        Assertions.assertEquals(userSettings.getContent(), settings);

        Assertions.assertEquals(mapper.update(userSettings.getId(), invalidSettings), 1);

        userSettings = mapper.get(Scope.USER, 1L);
        Assertions.assertNotNull(userSettings);
        Assertions.assertEquals(userSettings.getContent(), invalidSettings);
    }
}
