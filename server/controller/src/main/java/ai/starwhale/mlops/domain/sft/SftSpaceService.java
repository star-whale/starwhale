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

import ai.starwhale.mlops.api.protocol.sft.SftSpaceVo;
import ai.starwhale.mlops.domain.sft.mapper.SftSpaceMapper;
import ai.starwhale.mlops.domain.sft.po.SftSpaceEntity;
import ai.starwhale.mlops.domain.user.UserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SftSpaceService {

    final SftSpaceMapper sftSpaceMapper;
    final UserService userService;

    public SftSpaceService(SftSpaceMapper sftSpaceMapper, UserService userService) {
        this.sftSpaceMapper = sftSpaceMapper;
        this.userService = userService;
    }

    public void createSpace(Long projectId, String name, String description, Long userId) {
        sftSpaceMapper.add(
                SftSpaceEntity.builder()
                        .projectId(projectId)
                        .name(name)
                        .description(description)
                        .ownerId(userId)
                        .build()
        );
    }

    public PageInfo<SftSpaceVo> listSpace(Long projectId, Integer pageNum, Integer pageSize) {
        try (var ph = PageHelper.startPage(pageNum, pageSize)) {
            return PageInfo.of(sftSpaceMapper.list(projectId)
                                       .stream()
                                       .map(spaceEntity -> SftSpaceVo.builder()
                                               .id(spaceEntity.getId())
                                               .name(spaceEntity.getName())
                                               .description(spaceEntity.getDescription())
                                               .owner(userService.findUserById(spaceEntity.getOwnerId()))
                                               .createdTime(spaceEntity.getCreatedTime().getTime())
                                               .build())
                                       .collect(Collectors.toList()));
        }
    }

    public void updateSpace(Long spaceId, String name, String description) {
        sftSpaceMapper.update(spaceId, name, description);
    }
}
