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
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "Project object", title = "Project")
@Validated
public class ProjectVo implements Serializable {

    @NotNull
    private String id;

    @NotNull
    private String name;

    private String description;

    @NotNull
    private String privacy;

    @NotNull
    private Long createdTime;

    @NotNull
    private UserVo owner;

    private StatisticsVo statistics;

    public static ProjectVo empty() {
        return new ProjectVo("", "", "",
                Privacy.PRIVATE.toString(), -1L, UserVo.empty(), StatisticsVo.empty());
    }

    public static ProjectVo system() {
        return new ProjectVo("0", "SYSTEM", null,
                Privacy.PUBLIC.toString(), -1L, UserVo.empty(), StatisticsVo.empty());
    }

    public static ProjectVo fromBo(Project project, IdConverter idConvertor) {
        if (project == null) {
            return ProjectVo.empty();
        }
        if (project.getId() == 0) {
            return ProjectVo.system();
        }

        return ProjectVo.builder()
                .id(idConvertor.convert(project.getId()))
                .name(project.getName())
                .owner(UserVo.from(project.getOwner(), idConvertor))
                .createdTime(project.getCreatedTime().getTime())
                .privacy(project.getPrivacy().name())
                .description(project.getDescription())
                .statistics(StatisticsVo.empty())
                .build();
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
                .name(entity.getProjectName()).owner(Optional.ofNullable(owner).orElse(UserVo.empty()))
                .createdTime(entity.getCreatedTime().getTime())
                .privacy(Privacy.fromValue(entity.getPrivacy()).name())
                .description(entity.getProjectDescription())
                .statistics(StatisticsVo.empty())
                .build();
    }
}
