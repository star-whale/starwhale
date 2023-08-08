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

package ai.starwhale.mlops.domain.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.report.bo.CreateParam;
import ai.starwhale.mlops.domain.report.mapper.ReportMapper;
import ai.starwhale.mlops.domain.report.po.ReportEntity;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReportServiceTest {
    private ReportMapper reportMapper = mock(ReportMapper.class);
    private ReportDao reportDao = mock(ReportDao.class);
    private ProjectService projectService = mock(ProjectService.class);
    private UserService userService = mock(UserService.class);
    private ReportConverter reportConverter = new ReportConverter(userService);
    private TrashService trashService = mock(TrashService.class);
    private ReportService reportService;

    @BeforeEach
    public void setup() {
        given(projectService.getProjectId("p-1")).willReturn(1L);
        given(userService.currentUserDetail()).willReturn(User.builder().id(1L).build());
        reportService = new ReportService(
                reportMapper,
                reportDao,
                reportConverter,
                new IdConverter(),
                projectService,
                userService,
                trashService
        );
    }

    @Test
    public void testCreate() {
        reportService.create(
                CreateParam.builder()
                        .title("title")
                        .projectUrl("p-1")
                        .description("desc")
                        .content("content")
                        .shared(true)
                        .build()
        );
        verify(reportMapper, times(1)).insert(any());
    }

    @Test
    public void testPreview() {
        var entity = ReportEntity.builder()
                .id(1L)
                .uuid("uuid")
                .content("content")
                .shared(false)
                .createdTime(new Date())
                .modifiedTime(new Date())
                .build();
        given(reportMapper.selectByUuid(anyString())).willReturn(entity);
        assertThrows(SwValidationException.class, () -> reportService.getReportByUuidForPreview("uuid"));

        entity.setShared(true);
        assertNotNull(reportService.getReportByUuidForPreview("uuid"));
    }

}
