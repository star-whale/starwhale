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

package ai.starwhale.mlops.domain.project;

import ai.starwhale.mlops.api.protocol.project.ProjectVo;
import ai.starwhale.mlops.api.protocol.project.StatisticsVo;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProjectConvertor implements Convertor<ProjectEntity, ProjectVo> {

    private final IdConvertor idConvertor;

    private final UserConvertor userConvertor;

    private final LocalDateTimeConvertor localDateTimeConvertor;

    public ProjectConvertor(IdConvertor idConvertor,
            UserConvertor userConvertor,
            LocalDateTimeConvertor localDateTimeConvertor) {
        this.idConvertor = idConvertor;
        this.userConvertor = userConvertor;
        this.localDateTimeConvertor = localDateTimeConvertor;
    }


    @Override
    public ProjectVo convert(ProjectEntity entity) throws ConvertException {
        if (entity == null) {
            return ProjectVo.empty();
        }
        if (entity.getId() == 0) {
            return ProjectVo.system();
        }
        return ProjectVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .name(entity.getProjectName())
                .owner(userConvertor.convert(entity.getOwner()))
                .createdTime(localDateTimeConvertor.convert(entity.getCreatedTime()))
                .privacy(Privacy.fromValue(entity.getPrivacy()).name())
                .description(entity.getDescription())
                .statistics(StatisticsVo.empty())
                .build();
    }

    @Override
    public ProjectEntity revert(ProjectVo vo) throws ConvertException {
        Objects.requireNonNull(vo, "vo");
        return ProjectEntity.builder()
                .id(idConvertor.revert(vo.getId()))
                .projectName(vo.getName())
                .description(vo.getDescription())
                .build();
    }
}
