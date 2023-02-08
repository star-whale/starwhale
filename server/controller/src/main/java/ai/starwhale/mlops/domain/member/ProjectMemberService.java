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

import ai.starwhale.mlops.api.protocol.user.ProjectMemberVo;
import ai.starwhale.mlops.api.protocol.user.SystemRoleVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.member.bo.ProjectMember;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.mapper.ProjectMemberMapper;
import ai.starwhale.mlops.domain.project.po.ProjectMemberEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProjectMemberService {

    private final ProjectMemberMapper projectMemberMapper;
    private final UserService userService;
    private final ProjectService projectService;
    private final IdConverter idConverter;


    public ProjectMemberService(ProjectMemberMapper projectMemberMapper, UserService userService,
            ProjectService projectService, IdConverter idConverter) {
        this.projectMemberMapper = projectMemberMapper;
        this.userService = userService;
        this.projectService = projectService;
        this.idConverter = idConverter;
    }

    public List<ProjectMemberVo> listProjectMembers(String projectUrl) {
        Long projectId = projectService.getProjectId(projectUrl);
        List<ProjectMemberEntity> entities = projectMemberMapper.listByProject(projectId);
        return entities.stream()
                .map(entity -> ProjectMemberVo.builder()
                        .id(idConverter.convert(entity.getId()))
                        .role(userService.findRoleById(entity.getRoleId()))
                        .user(userService.findUserById(entity.getUserId()))
                        .project(projectService.getProjectVo(projectId))
                        .build()).collect(Collectors.toList());
    }

    public List<ProjectMember> listProjectMembersOfUser(Long userId) {
        List<ProjectMemberEntity> list = projectMemberMapper.listByUser(userId);
        return list.stream().map(entity -> ProjectMember.builder()
                .id(entity.getId())
                .project(projectService.findProject(entity.getProjectId()))
                .user(userService.loadUserById(entity.getUserId()))
                .role(userService.findRole(entity.getRoleId()))
                .build()
        ).collect(Collectors.toList());
    }

    public List<SystemRoleVo> listSystemMembers() {
        List<ProjectMemberEntity> entities = projectMemberMapper.listByProject(0L);

        return entities.stream()
                .map(entity -> SystemRoleVo.builder()
                        .id(idConverter.convert(entity.getId()))
                        .user(userService.findUserById(entity.getUserId()))
                        .role(userService.findRoleById(entity.getRoleId()))
                        .build())
                .collect(Collectors.toList());
    }

    public Role getUserRoleInProject(Long projectId, Long userId) {
        ProjectMemberEntity member = projectMemberMapper.findByUserAndProject(userId, projectId);
        if (member == null) {
            return null;
        }
        return userService.findRole(member.getRoleId());
    }

    public Boolean addProjectMember(String projectUrl, Long userId, Long roleId) {
        Long projectId = projectService.getProjectId(projectUrl);
        return addProjectMember(projectId, userId, roleId);
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

    public Boolean modifyProjectMember(String projectUrl, Long projectRoleId, Long roleId) {
        int res = projectMemberMapper.updateRole(projectRoleId, roleId);
        log.info("Project Role has been modified ID={}", projectRoleId);
        return res > 0;
    }

    public Boolean deleteProjectMember(String projectUrl, Long projectRoleId) {
        int res = projectMemberMapper.delete(projectRoleId);
        log.info("Project Role has been deleted ID={}", projectRoleId);
        return res > 0;
    }
}
