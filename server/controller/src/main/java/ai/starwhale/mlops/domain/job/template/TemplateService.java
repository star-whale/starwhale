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

package ai.starwhale.mlops.domain.job.template;

import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.template.bo.Template;
import ai.starwhale.mlops.domain.job.template.mapper.TemplateMapper;
import ai.starwhale.mlops.domain.job.template.po.TemplateEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SwValidationException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;


@Service
public class TemplateService {
    private final TemplateMapper mapper;
    private final UserService userService;
    private final ProjectService projectService;
    private final JobDao jobDao;

    public TemplateService(
            TemplateMapper mapper, UserService userService, ProjectService projectService, JobDao jobDao) {
        this.mapper = mapper;
        this.userService = userService;
        this.projectService = projectService;
        this.jobDao = jobDao;
    }

    public boolean add(String projectUrl, String jobUrl, String name) {
        var currentUser = userService.currentUserDetail();
        var project = projectService.findProject(projectUrl);
        var job = jobDao.findJob(jobUrl);
        if (mapper.selectExists(project.getId(), name) > 0) {
            throw new SwValidationException(
                    SwValidationException.ValidSubject.JOB, "template name already exists in this project");
        }
        return mapper.insert(TemplateEntity.builder()
                .name(name)
                .jobId(job.getId())
                .projectId(project.getId())
                .ownerId(currentUser.getId())
                .build()
        ) > 0;
    }

    public boolean delete(String projectUrl, Long id) {
        var project = projectService.findProject(projectUrl);
        return mapper.delete(id, project.getId()) > 0;
    }

    public Template get(String projectUrl, Long id) {
        var project = projectService.findProject(projectUrl);
        return Template.fromEntity(mapper.selectById(id, project.getId()));
    }

    public List<Template> listAll(String projectUrl) {
        var project = projectService.findProject(projectUrl);
        return mapper.select(project.getId(), -1).stream()
                .map(Template::fromEntity).collect(Collectors.toList());
    }

    public List<Template> listRecently(String projectUrl, int limit) {
        var project = projectService.findProject(projectUrl);
        // TODO impl real recently used
        return mapper.select(project.getId(), limit).stream()
                .map(Template::fromEntity).collect(Collectors.toList());
    }
}
