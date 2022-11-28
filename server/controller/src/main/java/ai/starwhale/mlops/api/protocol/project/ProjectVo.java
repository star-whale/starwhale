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

package ai.starwhale.mlops.api.protocol.project;

import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "Project object", title = "Project")
@Validated
public class ProjectVo implements Serializable {

    private String id;

    private String name;

    private String description;

    private String privacy;

    private Long createdTime;

    private UserVo owner;

    private StatisticsVo statistics;

    public static ProjectVo empty() {
        return new ProjectVo("", "", "",
                Privacy.PRIVATE.toString(), -1L, UserVo.empty(), StatisticsVo.empty());
    }

    public static ProjectVo system() {
        return new ProjectVo("0", "SYSTEM", "System",
                Privacy.PUBLIC.toString(), -1L, UserVo.empty(), StatisticsVo.empty());
    }

    public static ProjectVo fromEntity(ProjectEntity entity, IdConverter idConvertor, UserVo owner) {
        if (entity == null) {
            return ProjectVo.empty();
        }
        if (entity.getId() == 0) {
            return ProjectVo.system();
        }
        return ProjectVo.builder()
                .id(idConvertor.convert(entity.getId()))
                .name(entity.getProjectName())
                .owner(Optional.of(owner).orElse(UserVo.empty()))
                .createdTime(entity.getCreatedTime().getTime())
                .privacy(Privacy.fromValue(entity.getPrivacy()).name())
                .description(entity.getProjectDescription())
                .statistics(StatisticsVo.empty())
                .build();
    }
}
