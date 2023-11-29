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

package ai.starwhale.mlops.domain.ft;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.ft.FineTuneSpaceVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.domain.ft.mapper.FineTuneSpaceMapper;
import ai.starwhale.mlops.domain.ft.po.FineTuneSpaceEntity;
import ai.starwhale.mlops.domain.user.UserService;
import com.github.pagehelper.PageInfo;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FineTuneSpaceServiceTest {

    FineTuneSpaceMapper fineTuneSpaceMapper;
    UserService userService;

    FineTuneSpaceService fineTuneSpaceService;

    @BeforeEach
    public void setup() {
        fineTuneSpaceMapper = mock(FineTuneSpaceMapper.class);
        userService = mock(UserService.class);
        fineTuneSpaceService = new FineTuneSpaceService(fineTuneSpaceMapper, userService);
    }

    @Test
    void createSpace() {
        ArgumentCaptor<FineTuneSpaceEntity> argumentCaptor = ArgumentCaptor.forClass(FineTuneSpaceEntity.class);
        fineTuneSpaceService.createSpace(1L, "sn", "desc", 2L);
        verify(fineTuneSpaceMapper).add(argumentCaptor.capture());
        FineTuneSpaceEntity args = argumentCaptor.getValue();
        Assertions.assertEquals(1L, args.getProjectId());
        Assertions.assertEquals("sn", args.getName());
        Assertions.assertEquals("desc", args.getDescription());
        Assertions.assertEquals(2L, args.getOwnerId());

    }

    @Test
    void listSpace() {
        when(fineTuneSpaceMapper.list(any())).thenReturn(List.of(
                FineTuneSpaceEntity.builder().ownerId(1L).build()
        ));
        when(userService.findUserById(1L)).thenReturn(UserVo.builder().build());
        PageInfo<FineTuneSpaceVo> fineTuneSpaceVoPageInfo = fineTuneSpaceService.listSpace(1L, 1, 1);
        Assertions.assertEquals(1L, fineTuneSpaceVoPageInfo.getSize());
    }

    @Test
    void updateSpace() {
        fineTuneSpaceService.updateSpace(1L, "nm", "ds");
        verify(fineTuneSpaceMapper).update(eq(1L), eq("nm"), eq("ds"));
    }

    @Test
    void spaceInfo() {
        when(fineTuneSpaceMapper.findById(any())).thenReturn(FineTuneSpaceEntity.builder().ownerId(1L).build());
        UserVo userVo = UserVo.builder().build();
        when(userService.findUserById(1L)).thenReturn(userVo);
        FineTuneSpaceVo fineTuneSpaceVo = fineTuneSpaceService.spaceInfo(1L);
        Assertions.assertEquals(userVo, fineTuneSpaceVo.getOwner());
    }
}