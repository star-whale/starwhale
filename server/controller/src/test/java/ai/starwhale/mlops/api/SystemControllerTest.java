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

package ai.starwhale.mlops.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.system.SystemVersionVo;
import ai.starwhale.mlops.api.protocol.system.UpgradeProgressVo;
import ai.starwhale.mlops.api.protocol.system.UpgradeRequest;
import ai.starwhale.mlops.domain.system.SystemService;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class SystemControllerTest {

    private SystemController controller;

    private SystemService systemService;

    private SystemSettingService systemSettingService;

    @BeforeEach
    public void setUp() {
        systemService = mock(SystemService.class);
        systemSettingService = mock(SystemSettingService.class);
        controller = new SystemController(systemService, systemSettingService);
    }

    @Test
    public void testListResourcePools() {
        given(systemService.listResourcePools())
                .willReturn(List.of(
                        ResourcePool.builder().build()
                ));

        var resp = controller.listResourcePools();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                isA(List.class),
                is(iterableWithSize(1))
        ));
    }

    @Test
    public void testGetCurrentVersion() {
        given(systemService.controllerVersion())
                .willReturn("version1");
        var resp = controller.getCurrentVersion();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                isA(SystemVersionVo.class),
                hasProperty("version", is("version1"))
        ));
    }

    @Test
    public void testGetLatestVersion() {
        var resp = controller.getLatestVersion();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                isA(SystemVersionVo.class)
        ));
    }

    @Test
    public void testGetUpgradeProgress() {
        var resp = controller.getUpgradeProgress();
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                isA(UpgradeProgressVo.class)
        ));
    }

    @Test
    public void testUpdateSetting() {
        when(systemSettingService.updateSetting("xs")).thenReturn("dss");
        Assertions.assertEquals(ResponseEntity.ok(Code.success.asResponse("dss")), controller.updateSetting("xs"));
    }

    @Test
    public void testQuerySetting() {
        when(systemSettingService.querySetting()).thenReturn("dss");
        Assertions.assertEquals(ResponseEntity.ok(Code.success.asResponse("dss")), controller.querySetting());
    }

    @Test
    public void testUpgrade() {
        when(systemService.upgrade(same("0.4.0"), same("server:0.4.0")))
                .thenReturn("pid");
        UpgradeRequest upgradeRequest = new UpgradeRequest();
        upgradeRequest.setImage("server:0.4.0");
        upgradeRequest.setVersion("0.4.0");
        var resp = controller.upgradeVersion(upgradeRequest);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
    }

}
