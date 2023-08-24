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
import ai.starwhale.mlops.api.protocol.user.ProjectMemberVo;
import ai.starwhale.mlops.api.protocol.user.RoleVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.member.MemberService;
import ai.starwhale.mlops.domain.member.bo.ProjectMember;
import ai.starwhale.mlops.domain.model.mapper.ModelVersionMapper;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.project.bo.Project.Privacy;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.project.mapper.ProjectVisitedMapper;
import ai.starwhale.mlops.domain.project.po.ObjectCountEntity;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.project.po.ProjectObjectCounts;
import ai.starwhale.mlops.domain.project.sort.Sort;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.Role;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
public class ProjectService implements ProjectAccessor, ApplicationContextAware {

    public static final String PROJECT_NAME_REGEX = "^[a-zA-Z][a-zA-Z\\d_-]{2,80}$";


    private final ProjectMapper projectMapper;

    private final ProjectVisitedMapper projectVisitedMapper;
    private final ProjectDao projectDao;
    private final RuntimeVersionMapper runtimeVersionMapper;
    private final ModelVersionMapper modelVersionMapper;
    private final DatasetVersionMapper datasetVersionMapper;

    private final MemberService memberService;

    private final IdConverter idConvertor;

    private final UserService userService;

    private static final String DELETE_SUFFIX = ".deleted";

    private final Map<Long, Visit> visitedProjectCacheMap = new ConcurrentHashMap<>();
    private static final long storageInterval = 1000;
    private ApplicationContext applicationContext;

    public ProjectService(
            ProjectMapper projectMapper,
            ProjectVisitedMapper projectVisitedMapper,
            ProjectDao projectDao,
            MemberService memberService,
            IdConverter idConvertor,
            UserService userService,
            RuntimeVersionMapper runtimeVersionMapper,
            ModelVersionMapper modelVersionMapper,
            DatasetVersionMapper datasetVersionMapper
    ) {
        this.projectMapper = projectMapper;
        this.projectVisitedMapper = projectVisitedMapper;
        this.projectDao = projectDao;
        this.memberService = memberService;
        this.idConvertor = idConvertor;
        this.userService = userService;
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.modelVersionMapper = modelVersionMapper;
        this.datasetVersionMapper = datasetVersionMapper;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Project findProject(Long id) {
        ProjectEntity entity = projectDao.findById(id);
        return toProject(entity);
    }

    public Project findProject(String projectUrl) {
        ProjectEntity entity = projectDao.getProject(projectUrl);
        return toProject(entity);
    }

    private Project toProject(ProjectEntity entity) {
        return Project.builder()
                .id(entity.getId())
                .name(entity.getProjectName())
                .privacy(Privacy.fromValue(entity.getPrivacy()))
                .createdTime(entity.getCreatedTime())
                .description(entity.getProjectDescription())
                .isDefault(Objects.equals(entity.getIsDefault(), 1))
                .isDeleted(Objects.equals(entity.getIsDeleted(), 1))
                .owner(entity.getOwnerId() == null ? null : userService.loadUserById(entity.getOwnerId()))
                .build();
    }

    public ProjectVo getProjectVo(Long projectId) {
        return ProjectVo.fromBo(findProject(projectId), idConvertor);
    }

    /**
     * Find a project by parameters.
     *
     * @param projectUrl Project URL must be set.
     * @return Optional of a ProjectVo object.
     */
    public ProjectVo getProjectVo(String projectUrl) {
        return ProjectVo.fromBo(findProject(projectUrl), idConvertor);
    }

    public PageInfo<ProjectVo> listProject(String projectName, OrderParams orderParams, User user) {
        boolean showAll = false;
        List<Role> sysRoles = userService.getProjectRolesOfUser(user, Project.system());
        for (Role sysRole : sysRoles) {
            if (sysRole.getAuthority().equals("OWNER")) {
                showAll = true;
                break;
            }
        }

        Sort sort = getSort(orderParams.getSort());

        List<ProjectEntity> entities = sort.list(projectName, user, showAll);
        List<Long> ids = entities.stream().map(ProjectEntity::getId).collect(Collectors.toList());
        Map<Long, ProjectObjectCounts> countMap = getObjectCountsOfProjects(
                ids);

        return new PageInfo<>(entities.stream().map(entity -> {
            ProjectVo vo = ProjectVo.fromEntity(entity, idConvertor,
                    userService.findUserById(entity.getOwnerId()));
            ProjectObjectCounts count = countMap.get(entity.getId());
            if (count != null) {
                vo.setStatistics(StatisticsVo.builder()
                        .modelCounts(count.getCountModel())
                        .datasetCounts(count.getCountDataset())
                        .runtimeCounts(count.getCountRuntime())
                        .memberCounts(count.getCountMember())
                        .evaluationCounts(count.getCountJob())
                        .build());
            }
            return vo;
        }).collect(Collectors.toList()));
    }

    private Sort getSort(String type) {
        try {
            return this.applicationContext.getBean(StrUtil.isNotEmpty(type) ? type : "visited", Sort.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new SwValidationException(ValidSubject.PROJECT, "Unknown sort type. " + type);
        }
    }

    public List<Project> listProjects() {
        List<ProjectEntity> list = projectMapper.listOfUser(null, null, null);
        return list.stream().map(this::toProject).collect(Collectors.toList());
    }

    /**
     * Create a new project
     *
     * @param project Object of the project to create.
     * @return ID of the project was created.
     */
    @Transactional
    public Long createProject(Project project) {
        Assert.notNull(project.getName(), "Project name must not be null");
        if (existProject(project.getName(), project.getOwner().getId())) {
            //project exists and has not been deleted
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT,
                            String.format("Project %s already exists", project.getName())),
                    HttpStatus.BAD_REQUEST);
        }

        ProjectEntity entity = ProjectEntity.builder()
                .projectName(project.getName())
                .ownerId(project.getOwner().getId())
                .privacy(project.getPrivacy().getValue())
                .projectDescription(project.getDescription())
                .isDefault(project.isDefault() ? 1 : 0)
                .build();
        projectMapper.insert(entity);
        memberService.addProjectMember(entity.getId(), entity.getOwnerId(), Role.NAME_OWNER);
        log.info("Project has been created. ID={}, NAME={}", entity.getId(), entity.getProjectName());
        return entity.getId();
    }

    /**
     * Delete a project
     *
     * @param projectUrl Project URL must be set.
     * @return Is the operation successful.
     */
    @Transactional
    public Boolean deleteProject(String projectUrl) {
        ProjectEntity entity = projectDao.getProject(projectUrl);
        if (entity == null) {
            throw new SwNotFoundException(ResourceType.PROJECT, "Unable to find project");
        }
        if (entity.getIsDefault() > 0) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT, "Default project cannot be deleted."),
                    HttpStatus.BAD_REQUEST);
        }
        entity.setProjectName(entity.getProjectName() + DELETE_SUFFIX + "." + entity.getId());
        projectMapper.update(entity);
        int res = projectMapper.remove(entity.getId());
        log.info("Project has been deleted. ID={}", entity.getId());
        return res > 0;
    }

    @Transactional
    public Long recoverProject(String projectUrl) {
        String projectName;
        Long ownerId;
        Long id;
        if (idConvertor.isId(projectUrl)) {
            id = idConvertor.revert(projectUrl);
            ProjectEntity entity = projectMapper.find(id);
            if (entity == null) {
                throw new SwNotFoundException(ResourceType.PROJECT,
                        "Recover project error. Project can not be found.");
            }
            projectName = entity.getProjectName().substring(0,
                    entity.getProjectName().lastIndexOf(DELETE_SUFFIX));
            ownerId = entity.getOwnerId();
            entity.setProjectName(projectName);
        } else {
            String[] arr = ProjectDao.splitProjectUrl(projectUrl);
            if (arr.length > 1) {
                projectName = arr[1];
                if (idConvertor.isId(arr[0])) {
                    ownerId = idConvertor.revert(arr[0]);
                } else {
                    ownerId = Optional.of(userService.loadUserByUsername(arr[0]))
                            .orElseThrow(() -> new SwNotFoundException(ResourceType.USER,
                                    "Recover project error. User can not be found. "))
                            .getId();
                }
            } else {
                projectName = projectUrl;
                ownerId = userService.currentUserDetail().getId();
            }

            // To restore projects by name, need to check whether there are duplicate names
            List<ProjectEntity> deletedProjects = projectMapper.listRemovedProjects(projectName + DELETE_SUFFIX,
                    ownerId);
            if (deletedProjects.size() > 1) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.PROJECT,
                                StrUtil.format("Recover project error. Duplicate names [%s] of deleted project. ",
                                        projectName)),
                        HttpStatus.BAD_REQUEST);
            } else if (deletedProjects.size() == 0) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.PROJECT,
                                StrUtil.format("Recover project error. Can not find deleted project [%s].",
                                        projectName)),
                        HttpStatus.BAD_REQUEST);
            }
            id = deletedProjects.get(0).getId();
        }

        // Check for duplicate names
        if (existProject(projectName, ownerId)) {
            throw new StarwhaleApiException(
                    new SwValidationException(ValidSubject.PROJECT,
                            String.format("Recover project error. Project %s already exists", projectName)),
                    HttpStatus.BAD_REQUEST);
        }
        projectMapper.update(ProjectEntity.builder()
                .id(id)
                .projectName(projectName)
                .build());
        projectMapper.recover(id);
        log.info("Project has been recovered. Name={}", projectName);
        return id;
    }

    @Transactional
    public Boolean updateProject(String projectUrl, String projectName, String description, String privacy) {
        ProjectEntity project = projectDao.getProject(projectUrl);
        Long projectId = project.getId();
        if (StrUtil.isNotEmpty(projectName)) {
            var ownerId = project.getOwnerId();
            ProjectEntity existProject = projectMapper.findByNameForUpdateAndOwner(projectName, ownerId);
            if (existProject != null && !Objects.equals(existProject.getId(), projectId)) {
                throw new StarwhaleApiException(
                        new SwValidationException(ValidSubject.PROJECT,
                                String.format("Project %s already exists", projectName)),
                        HttpStatus.BAD_REQUEST);
            }
        }

        var privacyEnum = Privacy.fromName(privacy);
        if (privacyEnum == Privacy.PRIVATE) {
            // make all the shared resource unshared
            runtimeVersionMapper.unShareRuntimeVersionWithinProject(projectId);
            modelVersionMapper.unShareModelVersionWithinProject(projectId);
            datasetVersionMapper.unShareDatesetVersionWithinProject(projectId);
        }

        ProjectEntity entity = ProjectEntity.builder()
                .id(projectId)
                .projectName(projectName)
                .projectDescription(description)
                .privacy(privacyEnum.getValue())
                .build();
        int res = projectMapper.update(entity);
        log.info("Project has been modified ID={}", entity.getId());
        return res > 0;
    }

    @Override
    public Long getProjectId(String projectUrl) {
        return findProject(projectUrl).getId();
    }


    private Boolean existProject(String projectName, Long userId) {
        ProjectEntity existProject = projectMapper.findExistingByNameAndOwner(projectName, userId);
        return existProject != null;
    }

    private Map<Long, ProjectObjectCounts> getObjectCountsOfProjects(List<Long> projectIds) {
        Map<Long, ProjectObjectCounts> map = Maps.newHashMap();
        if (projectIds.isEmpty()) {
            return map;
        }
        String ids = Joiner.on(",").join(projectIds);
        for (Long projectId : projectIds) {
            map.put(projectId, new ProjectObjectCounts());
        }

        List<ObjectCountEntity> modelCounts = projectMapper.countModel(ids);
        setCounts(modelCounts, map, ProjectObjectCounts::setCountModel);

        List<ObjectCountEntity> datasetCounts = projectMapper.countDataset(ids);
        setCounts(datasetCounts, map, ProjectObjectCounts::setCountDataset);

        List<ObjectCountEntity> runtimeCounts = projectMapper.countRuntime(ids);
        setCounts(runtimeCounts, map, ProjectObjectCounts::setCountRuntime);

        List<ObjectCountEntity> jobCounts = projectMapper.countJob(ids);
        setCounts(jobCounts, map, ProjectObjectCounts::setCountJob);

        List<ObjectCountEntity> memberCounts = projectMapper.countMember(ids);
        setCounts(memberCounts, map, ProjectObjectCounts::setCountMember);

        return map;
    }

    private void setCounts(List<ObjectCountEntity> list, Map<Long, ProjectObjectCounts> map, CountSetter setter) {
        for (ObjectCountEntity entity : list) {
            if (map.containsKey(entity.getProjectId())) {
                setter.set(map.get(entity.getProjectId()), entity.getCount());
            }
        }
    }

    interface CountSetter {

        void set(ProjectObjectCounts obj, Integer count);
    }

    public List<ProjectMemberVo> listProjectMembersInProject(String projectUrl) {
        Project project = findProject(projectUrl);
        List<ProjectMember> members = memberService.listProjectMembersInProject(project.getId());
        return members.stream().map(member -> ProjectMemberVo.builder()
                .id(idConvertor.convert(member.getId()))
                .project(ProjectVo.fromBo(project, idConvertor))
                .user(UserVo.from(userService.loadUserById(member.getUserId()), idConvertor))
                .role(RoleVo.fromBo(userService.findRole(member.getRoleId()), idConvertor))
                .build()).collect(Collectors.toList());
    }

    public List<ProjectMemberVo> listProjectMemberOfCurrentUser(String projectUrl) {
        User user = userService.currentUserDetail();
        List<ProjectMember> members;
        if (StrUtil.isNotEmpty(projectUrl)) {
            Long projectId = getProjectId(projectUrl);
            ProjectMember member = memberService.getUserMemberInProject(projectId, user.getId());
            members = (member == null ? List.of() : List.of(member));
        } else {
            members = memberService.listProjectMembersOfUser(user.getId());
        }
        return members.stream().map(member -> ProjectMemberVo.builder()
                .id(idConvertor.convert(member.getId()))
                .project(ProjectVo.builder().id(idConvertor.convert(member.getProjectId())).build())
                .user(UserVo.from(user, idConvertor))
                .role(RoleVo.fromBo(userService.findRole(member.getRoleId()), idConvertor))
                .build()).collect(Collectors.toList());
    }

    public Boolean addProjectMember(String projectUrl, Long userId, Long roleId) {
        Long projectId = getProjectId(projectUrl);
        return memberService.addProjectMember(projectId, userId, roleId);
    }


    public void visit(String projectUrl) {
        if (!Objects.equals("0", projectUrl)) {
            Long projectId = getProjectId(projectUrl);
            Long userId = userService.currentUserDetail().getId();

            Visit visit = new Visit(projectId, System.currentTimeMillis());
            Visit lastVisit = visitedProjectCacheMap.get(userId);

            if (lastVisit == null || needStorage(visit, lastVisit)) {
                visitedProjectCacheMap.put(userId, visit);
                projectVisitedMapper.insert(userId, projectId);
            }
        }
    }


    private boolean needStorage(Visit current, Visit previous) {
        return !Objects.equals(current.projectId, previous.projectId)
                && current.time > previous.time + storageInterval;
    }

    @AllArgsConstructor
    static class Visit {

        Long projectId;
        long time;
    }
}
