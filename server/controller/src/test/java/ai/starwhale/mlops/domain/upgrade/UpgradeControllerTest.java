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

package ai.starwhale.mlops.domain.upgrade;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.system.LatestVersionVo;
import ai.starwhale.mlops.api.protocol.system.UpgradeRequest;
import ai.starwhale.mlops.domain.upgrade.bo.Version;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class UpgradeControllerTest {

    private UpgradeController controller;

    private UpgradeService upgradeService;

    @BeforeEach
    public void setUp() {
        upgradeService = mock(UpgradeService.class);
        controller = new UpgradeController(upgradeService);
    }

    @Test
    public void testUpgrade() {
        when(upgradeService.upgrade(any())).thenReturn(null);
        UpgradeRequest upgradeRequest = new UpgradeRequest();
        upgradeRequest.setImage("server:0.4.0");
        upgradeRequest.setVersion("0.4.0");
        var resp = controller.upgradeVersion(upgradeRequest);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testGetLatestVersion() {
        given(upgradeService.getLatestVersion())
                .willReturn(new Version("", ""));
        var resp = controller.getLatestVersion();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                isA(LatestVersionVo.class)
        ));
    }

    @Test
    public void testGetUpgradeProgress() {
        var resp = controller.getUpgradeProgress();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue()
        ));
    }
}
