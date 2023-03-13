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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.domain.upgrade.bo.UpgradeLog;
import ai.starwhale.mlops.domain.upgrade.mapper.ServerStatusMapper;
import ai.starwhale.mlops.domain.upgrade.mapper.UpgradeLogMapper;
import ai.starwhale.mlops.domain.upgrade.po.UpgradeLogEntity;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MySqlUpgradeAccessTest {

    private MySqlUpgradeAccess access;

    private ServerStatusMapper serverStatusMapper;
    private UpgradeLogMapper upgradeLogMapper;

    @BeforeEach
    public void setUp() {
        serverStatusMapper = mock(ServerStatusMapper.class);
        upgradeLogMapper = mock(UpgradeLogMapper.class);

        access = new MySqlUpgradeAccess(serverStatusMapper, upgradeLogMapper);
    }

    @Test
    public void testIsUpgrading() {
        given(serverStatusMapper.getModuleStatus(same("CONTROLLER")))
                .willReturn("UPGRADING");

        var res = access.isUpgrading();
        assertTrue(res);
    }

    @Test
    public void testSetStatusToUpgrading() {
        AtomicBoolean call = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> call.getAndSet(true))
                .when(serverStatusMapper).updateModule(anyString(), same("id"), same("UPGRADING"));
        access.setStatusToUpgrading("id");
        assertTrue(call.get());
    }

    @Test
    public void testSetStatusToNormal() {
        AtomicBoolean call = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> call.getAndSet(true))
                .when(serverStatusMapper)
                .updateModule(anyString(), same(""), same("NORMAL"));
        access.setStatusToNormal();
        assertTrue(call.get());
    }

    @Test
    public void testWriteLog() {
        AtomicBoolean call = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> call.getAndSet(true))
                .when(upgradeLogMapper)
                .insert(argThat(argument -> Objects.equals("uuid", argument.getProgressUuid())));

        access.writeLog(UpgradeLog.builder().progressUuid("uuid").build());
        assertTrue(call.get());
    }

    @Test
    public void testUpdateLog() {
        AtomicBoolean call = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> call.getAndSet(true))
                .when(upgradeLogMapper)
                .update(argThat(argument -> Objects.equals("uuid", argument.getProgressUuid())));

        access.updateLog(UpgradeLog.builder().progressUuid("uuid").build());
        assertTrue(call.get());
    }

    @Test
    public void testReadLog() {
        given(upgradeLogMapper.list(same("pro1")))
                .willReturn(List.of(
                        UpgradeLogEntity.builder()
                                .progressUuid("pro1")
                                .stepCurrent(1)
                                .stepTotal(2)
                                .title("step1")
                                .status("COMPLETE")
                                .build(),
                        UpgradeLogEntity.builder()
                                .progressUuid("pro1")
                                .stepCurrent(1)
                                .stepTotal(2)
                                .title("step2")
                                .status("START")
                                .build()
                ));

        var res = access.readLog("pro1");
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(2)),
                hasItem(hasProperty("title", is("step1"))),
                hasItem(hasProperty("title", is("step2")))
        ));
    }
}
