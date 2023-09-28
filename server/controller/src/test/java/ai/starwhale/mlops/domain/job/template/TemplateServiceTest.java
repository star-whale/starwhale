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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.template.mapper.TemplateMapper;
import ai.starwhale.mlops.domain.job.template.po.TemplateEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class TemplateServiceTest {

    private TemplateMapper mapper;
    private UserService userService;
    private ProjectService projectService;
    private JobDao jobDao;
    private TemplateService templateService;

    @BeforeEach
    public void setup() {
        mapper = mock(TemplateMapper.class);
        userService = mock(UserService.class);
        projectService = mock(ProjectService.class);
        jobDao = mock(JobDao.class);
        templateService = new TemplateService(mapper, userService, projectService, jobDao);
    }

    @Test
    public void testAdd() {
        given(userService.currentUserDetail()).willReturn(User.builder().id(1L).build());
        given(projectService.findProject(anyString())).willReturn(Project.builder().id(11L).build());
        given(jobDao.findJob(anyString())).willReturn(Job.builder().id(111L).build());


        // template exists
        given(mapper.selectExists(11L, "t-name")).willReturn(1);
        assertThrows(SwValidationException.class,
                () -> templateService.add("p-1", "j-11", "t-name"));

        // normal
        given(mapper.selectExists(11L, "t-name")).willReturn(0);
        given(mapper.insert(any(TemplateEntity.class))).willReturn(1);
        assertTrue(templateService.add("p-1", "j-11", "t-name"));
    }

    @Test
    public void testDelete() {
        given(mapper.remove(1L)).willReturn(1);
        assertTrue(templateService.delete(1L));
    }

    @Test
    public void testGet() {
        given(mapper.selectById(1L)).willReturn(
                TemplateEntity.builder().id(1L).projectId(11L).jobId(1L).build()
        );

        assertNotNull(templateService.get(1L));
    }

    @Test
    public void testList() {
        given(projectService.findProject(anyString())).willReturn(Project.builder().id(11L).build());
        given(mapper.selectAll(11L)).willReturn(List.of(
                TemplateEntity.builder().id(1L).projectId(11L).jobId(1L).build(),
                TemplateEntity.builder().id(2L).projectId(11L).jobId(2L).build(),
                TemplateEntity.builder().id(3L).projectId(11L).jobId(3L).build()
        ));

        var all = templateService.listAll("p-1");
        assertEquals(all.size(), 3);
    }
}
