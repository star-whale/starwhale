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

package ai.starwhale.mlops.domain.member.bo;

import ai.starwhale.mlops.domain.project.po.ProjectMemberEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMember {

    private Long id;

    private Long projectId;

    private Long userId;

    private Long roleId;

    public static ProjectMember fromEntity(ProjectMemberEntity entity) {
        return ProjectMember.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .userId(entity.getUserId())
                .roleId(entity.getRoleId())
                .build();
    }
}
