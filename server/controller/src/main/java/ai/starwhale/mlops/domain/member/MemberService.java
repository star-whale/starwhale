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

package ai.starwhale.mlops.domain.member;

import ai.starwhale.mlops.domain.member.bo.ProjectMember;
import ai.starwhale.mlops.domain.project.mapper.ProjectMemberMapper;
import ai.starwhale.mlops.domain.project.po.ProjectMemberEntity;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MemberService {

    private final ProjectMemberMapper projectMemberMapper;


    public MemberService(ProjectMemberMapper projectMemberMapper) {
        this.projectMemberMapper = projectMemberMapper;
    }

    public List<ProjectMember> listProjectMembersInProject(Long projectId) {
        List<ProjectMemberEntity> list = projectMemberMapper.listByProject(projectId);
        return list.stream().map(ProjectMember::fromEntity).collect(Collectors.toList());
    }

    public List<ProjectMember> listProjectMembersOfUser(Long userId) {
        List<ProjectMemberEntity> list = projectMemberMapper.listByUser(userId);
        return list.stream().map(ProjectMember::fromEntity).collect(Collectors.toList());
    }

    public ProjectMember getUserMemberInProject(Long projectId, Long userId) {
        ProjectMemberEntity member = projectMemberMapper.findByUserAndProject(userId, projectId);
        if (member == null) {
            return null;
        }
        return ProjectMember.fromEntity(member);
    }


    public Boolean addProjectMember(Long projectId, Long userId, Long roleId) {
        ProjectMemberEntity entity = ProjectMemberEntity.builder()
                .userId(userId)
                .roleId(roleId)
                .projectId(projectId)
                .build();
        int res = projectMemberMapper.insert(entity);
        log.info("Project Role has been created ID={}", entity.getId());
        return res > 0;
    }

    public Boolean addProjectMember(Long projectId, Long userId, String roleName) {
        int res = projectMemberMapper.insertByRoleName(userId, projectId, roleName);
        return res > 0;
    }

    public Boolean modifyProjectMember(Long projectRoleId, Long roleId) {
        int res = projectMemberMapper.updateRole(projectRoleId, roleId);
        log.info("Project Role has been modified ID={}", projectRoleId);
        return res > 0;
    }

    public Boolean deleteProjectMember(Long projectRoleId) {
        int res = projectMemberMapper.delete(projectRoleId);
        log.info("Project Role has been deleted ID={}", projectRoleId);
        return res > 0;
    }
}
