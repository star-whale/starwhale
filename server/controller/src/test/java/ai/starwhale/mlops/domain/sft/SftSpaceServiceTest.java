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

package ai.starwhale.mlops.domain.sft;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.domain.sft.mapper.SftSpaceMapper;
import ai.starwhale.mlops.domain.sft.po.SftSpaceEntity;
import ai.starwhale.mlops.domain.user.UserService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SftSpaceServiceTest {

    SftSpaceMapper sftSpaceMapper;
    UserService userService;

    SftSpaceService sftSpaceService;

    @BeforeEach
    public void setup() {
        sftSpaceMapper = mock(SftSpaceMapper.class);
        userService = mock(UserService.class);
        sftSpaceService = new SftSpaceService(sftSpaceMapper, userService);
    }

    @Test
    void createSpace() {
        ArgumentCaptor<SftSpaceEntity> argumentCaptor = ArgumentCaptor.forClass(SftSpaceEntity.class);
        sftSpaceService.createSpace(1L, "sn", "desc", 2L);
        verify(sftSpaceMapper).add(argumentCaptor.capture());
        SftSpaceEntity args = argumentCaptor.getValue();
        Assertions.assertEquals(1L, args.getProjectId());
        Assertions.assertEquals("sn", args.getName());
        Assertions.assertEquals("desc", args.getDescription());
        Assertions.assertEquals(2L, args.getOwnerId());

    }

    @Test
    void listSpace() {
        when(sftSpaceMapper.list(any())).thenReturn(List.of(
                SftSpaceEntity.builder().build()
        ));
        when(userService.findUserById(1L)).thenReturn(UserVo.builder().build());
        when(userService.findUserById(2L)).thenReturn(UserVo.builder().build());
    }

    @Test
    void updateSpace() {
        sftSpaceService.updateSpace(1L, "nm", "ds");
        verify(sftSpaceMapper).update(eq(1L), eq("nm"), eq("ds"));
    }
}