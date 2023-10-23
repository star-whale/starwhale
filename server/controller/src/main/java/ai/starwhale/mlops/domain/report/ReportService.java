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

import ai.starwhale.mlops.api.protocol.report.ReportVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.report.bo.CreateParam;
import ai.starwhale.mlops.domain.report.bo.QueryParam;
import ai.starwhale.mlops.domain.report.bo.UpdateParam;
import ai.starwhale.mlops.domain.report.mapper.ReportMapper;
import ai.starwhale.mlops.domain.report.po.ReportEntity;
import ai.starwhale.mlops.domain.trash.Trash;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwValidationException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.UUID;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ReportService {
    private final ReportMapper reportMapper;
    private final ReportDao reportDao;
    private final ReportConverter reportConverter;
    private final ProjectService projectService;
    private final UserService userService;
    private final TrashService trashService;
    @Setter
    private BundleManager bundleManager;


    public ReportService(ReportMapper reportMapper,
                         ReportDao reportDao,
                         ReportConverter reportConverter,
                         IdConverter idConvertor,
                         ProjectService projectService,
                         UserService userService,
                         TrashService trashService) {
        this.reportMapper = reportMapper;
        this.reportDao = reportDao;
        this.reportConverter = reportConverter;
        this.projectService = projectService;
        this.userService = userService;
        this.trashService = trashService;
        this.bundleManager = new BundleManager(
                idConvertor, null, projectService, reportDao, null, null);
    }

    public Long create(CreateParam createParam) {
        var projectId = projectService.getProjectId(createParam.getProjectUrl());
        var user = userService.currentUserDetail();
        var entity = ReportEntity.builder()
                .uuid(String.valueOf(UUID.randomUUID()))
                .title(createParam.getTitle())
                .description(createParam.getDescription())
                .content(createParam.getContent())
                .projectId(projectId)
                .ownerId(user.getId())
                .build();
        reportMapper.insert(entity);
        return entity.getId();
    }

    public Boolean update(UpdateParam updateParam) {
        var updateEntity = ReportEntity.builder()
                .id(updateParam.getReportId())
                .title(updateParam.getTitle())
                .description(updateParam.getDescription())
                .content(updateParam.getContent()).shared(updateParam.getShared())
                .build();
        return reportMapper.update(updateEntity) > 0;
    }

    public Boolean shared(UpdateParam updateParam) {
        return reportMapper.updateShared(updateParam.getReportId(), updateParam.getShared()) > 0;
    }

    @Transactional
    public Boolean delete(QueryParam query) {
        Trash trash = Trash.builder()
                .projectId(projectService.getProjectId(query.getProjectUrl()))
                .objectId(query.getReportId())
                .type(Trash.Type.REPORT)
                .build();
        trashService.moveToRecycleBin(trash, userService.currentUserDetail());
        return RemoveManager.create(bundleManager, reportDao)
                .removeBundle(BundleUrl.create(query.getProjectUrl(), String.valueOf(query.getReportId())));
    }

    public PageInfo<ReportVo> listReport(QueryParam query, PageParams pageParams) {
        var projectId = projectService.getProjectId(query.getProjectUrl());
        try (var pager = PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize())) {
            var reports = reportMapper.selectByProject(query.getTitle(), projectId);
            return PageUtil.toPageInfo(reports, reportConverter::convert);
        }
    }

    public ReportVo getReport(QueryParam query) {
        ReportEntity entity = reportMapper.selectById(query.getReportId());
        if (entity == null) {
            throw new SwNotFoundException(SwNotFoundException.ResourceType.BUNDLE,
                    String.format("Unable to find report %s", query.getReportId()));
        }
        return reportConverter.convert(entity);
    }

    public ReportVo getReportByUuidForPreview(String uuid) {
        ReportEntity entity = reportMapper.selectByUuid(uuid);
        if (entity == null || entity.getIsDeleted()) {
            throw new SwNotFoundException(SwNotFoundException.ResourceType.BUNDLE,
                    String.format("Unable to find report %s", uuid));
        }
        if (!entity.getShared()) {
            throw new SwValidationException(SwValidationException.ValidSubject.REPORT,
                    String.format("Report %s doesn't shared", uuid));
        }
        return reportConverter.convert(entity);
    }
}
