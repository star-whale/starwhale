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

package ai.starwhale.mlops.domain.panel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.panel.mapper.PanelSettingMapper;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PanelSettingServiceTest {

    private PanelSettingService service;
    private PanelSettingMapper panelSettingMapper;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        var idConvertor = new IdConverter();
        userService = mock(UserService.class);
        panelSettingMapper = mock(PanelSettingMapper.class);
        service = new PanelSettingService(
            userService,
            panelSettingMapper,
            idConvertor
        );
    }

    @Test
    public void testGetSetting() {
        var expect = "bar";
        given(panelSettingMapper.get(42, "foo")).willReturn(expect);
        var resp = service.getSetting("42", "foo");
        assertThat(resp, is(expect));
    }

    @Test
    public void testSaveSetting() {
        given(userService.currentUserDetail()).willReturn(User.builder().id(1L).build());
        service.saveSetting("42", "foo", "bar");
        verify(panelSettingMapper).set(1L, 42L, "foo", "bar");
    }
}
