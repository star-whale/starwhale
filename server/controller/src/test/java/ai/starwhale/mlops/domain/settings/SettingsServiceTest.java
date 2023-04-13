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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.domain.settings.mapper.SettingsMapper;
import ai.starwhale.mlops.domain.settings.po.SettingsEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SettingsServiceTest {

    private final SettingsMapper mapper;
    private final UserService userService;
    private final SettingsService service;

    String settingsStr1 = "---\n"
            + "env:\n"
            + "  SW_ENV_1: 'sw_env_1'\n"
            + "  SW_ENV_2: 'sw_env_2'\n"
            + "  SW_ENV_3: 'sw_env_3'\n"
            + "others:\n"
            + "- OTH: ''";
    String settingsStr2 = "---\n"
            + "env:\n"
            + "  CUSTOM_ENV_1: 'custom_env_1'\n"
            + "  CUSTOM_ENV_2: 'custom_env_2'\n"
            + "  CUSTOM_ENV_3: 'custom_env_3'\n"
            + "others:\n"
            + "- OTH: ''";

    String invalidSettingsStr = "123";

    public SettingsServiceTest() {
        this.mapper = mock(SettingsMapper.class);
        this.userService = mock(UserService.class);
        this.service = new SettingsService(userService, mapper);
    }

    @BeforeEach
    public void setUp() {
        given(userService.currentUserDetail()).willReturn(User.builder().id(1L).build());
    }


    @Test
    public void testQueryAndInsert() {
        service.updateUserSettings(settingsStr2);
        verify(mapper, times(1)).get(Scope.USER, 1L);
        verify(mapper, times(0)).update(any(), any());
        verify(mapper, times(1)).insert(1L, settingsStr2, Scope.USER);
    }

    @Test
    public void testQueryAndUpdate() {
        given(mapper.get(Scope.USER, 1L)).willReturn(
                SettingsEntity.builder().id(1L).scope(Scope.USER).content(settingsStr1).build());
        // query
        var settings = service.queryUserSettings();
        assertThat(settings.getEnv(), is(
                Map.of(
                    "SW_ENV_1", "sw_env_1",
                    "SW_ENV_2", "sw_env_2",
                    "SW_ENV_3", "sw_env_3")));
        // update
        service.updateUserSettings(settingsStr2);
        verify(mapper, times(2)).get(Scope.USER, 1L);
        verify(mapper, times(1)).update(1L, settingsStr2);
        verify(mapper, times(0)).insert(any(), any(), any());

    }

    @Test
    public void testExceptionWhenUpdate() {
        given(mapper.get(Scope.USER, 1L)).willReturn(
                SettingsEntity.builder().id(1L).scope(Scope.USER).content(settingsStr1).build());
        Assertions.assertThrows(SwValidationException.class, () -> service.updateUserSettings(invalidSettingsStr));
    }
}
