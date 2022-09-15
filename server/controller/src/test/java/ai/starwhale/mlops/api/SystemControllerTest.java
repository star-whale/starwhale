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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.api.protocol.agent.AgentVo;
import ai.starwhale.mlops.api.protocol.system.ResourcePoolVo;
import ai.starwhale.mlops.api.protocol.system.SystemVersionVo;
import ai.starwhale.mlops.api.protocol.system.UpgradeProgressVo;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.system.SystemService;
import com.github.pagehelper.Page;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class SystemControllerTest {

    private SystemController controller;

    private SystemService systemService;

    @BeforeEach
    public void setUp() {
        systemService = mock(SystemService.class);
        controller = new SystemController(systemService);
    }

    @Test
    public void testListAgent() {
        given(systemService.listAgents(anyString(), any(PageParams.class)))
                .willAnswer(invocation -> {
                    PageParams pageParams = invocation.getArgument(1);
                    try (Page<AgentVo> page = new Page<>(pageParams.getPageNum(), pageParams.getPageSize())) {
                        page.add(AgentVo.builder().build());
                        return page.toPageInfo();
                    }
                });

        var resp = controller.listAgent("", 2, 5);
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
        assertThat(Objects.requireNonNull(resp.getBody()).getData(), allOf(
                notNullValue(),
                is(hasProperty("pageNum", is(2))),
                is(hasProperty("pageSize", is(5))),
                is(hasProperty("list", isA(List.class)))
        ));

    }

    @Test
    public void testListResourcePools() {
        given(systemService.listResourcePools())
                .willReturn(List.of(
                        ResourcePoolVo.builder().build()
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
    public void testSystemVersionAction() {
        var resp = controller.systemVersionAction("");
        assertThat(resp.getStatusCode(), is(HttpStatus.OK));
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

}
