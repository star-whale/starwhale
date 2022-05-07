/**
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

package ai.starwhale.mlops.domain.project;

import ai.starwhale.mlops.api.protocol.project.ProjectVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProjectConvertor implements Convertor<ProjectEntity, ProjectVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public ProjectVO convert(ProjectEntity entity) throws ConvertException {
        if(entity == null) {
            return ProjectVO.empty();
        }
        return ProjectVO.builder()
            .id(idConvertor.convert(entity.getId()))
            .name(entity.getProjectName())
            .owner(userConvertor.convert(entity.getOwner()))
            .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
            .isDefault(entity.getIsDefault() != 0)
            .build();
    }

    @Override
    public ProjectEntity revert(ProjectVO vo) throws ConvertException {
        Objects.requireNonNull(vo, "vo");
        return ProjectEntity.builder()
            .id(idConvertor.revert(vo.getId()))
            .projectName(vo.getName())
            .build();
    }
}
