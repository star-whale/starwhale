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

package ai.starwhale.mlops.domain.trash;

import ai.starwhale.mlops.api.protocol.trash.TrashVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.recover.RecoverAccessor;
import ai.starwhale.mlops.domain.bundle.recover.RecoverException;
import ai.starwhale.mlops.domain.bundle.recover.RecoverManager;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.model.ModelDao;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.report.ReportDao;
import ai.starwhale.mlops.domain.runtime.RuntimeDao;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.bo.TrashQuery;
import ai.starwhale.mlops.domain.trash.mapper.TrashMapper;
import ai.starwhale.mlops.domain.trash.po.TrashPo;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TrashService {

    private final TrashMapper trashMapper;
    private final UserService userService;
    private final ProjectService projectService;
    private final ModelDao modelDao;
    private final DatasetDao datasetDao;
    private final RuntimeDao runtimeDao;
    private final JobDao jobDao;
    private final ReportDao reportDao;
    private final IdConverter idConvertor;

    public TrashService(TrashMapper trashMapper, UserService userService, ProjectService projectService,
            ModelDao modelDao, DatasetDao datasetDao,
            RuntimeDao runtimeDao, JobDao jobDao, ReportDao reportDao, IdConverter idConvertor) {
        this.trashMapper = trashMapper;
        this.userService = userService;
        this.projectService = projectService;
        this.modelDao = modelDao;
        this.datasetDao = datasetDao;
        this.runtimeDao = runtimeDao;
        this.jobDao = jobDao;
        this.reportDao = reportDao;
        this.idConvertor = idConvertor;
    }

    public PageInfo<TrashVo> listTrash(TrashQuery query, PageParams pageParams, OrderParams orderParams) {
        Long projectId = projectService.getProjectId(query.getProjectUrl());
        Long operatorId = null;
        if (StrUtil.isNotEmpty(query.getOperator())) {
            if (idConvertor.isId(query.getOperator())) {
                operatorId = idConvertor.revert(query.getOperator());
            } else {
                projectId = userService.getUserId(query.getOperator());
            }
        }
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<TrashPo> poList = trashMapper.list(projectId, operatorId, query.getName(), query.getType());
        return PageUtil.toPageInfo(poList, this::toTrashVo);
    }

    @Transactional
    public Boolean recover(String projectUrl, Long trashId) {
        TrashPo trashPo = trashMapper.find(trashId);
        if (trashPo == null) {
            throw new SwValidationException(ValidSubject.TRASH, "Can not find trash.");
        }
        if (!trashPo.getProjectId().equals(projectService.getProjectId(projectUrl))) {
            throw new SwValidationException(ValidSubject.TRASH, "Project is not match.");
        }
        try {
            boolean res = RecoverManager.create(getRecoverAccessor(Type.valueOf(trashPo.getTrashType())))
                    .recoverBundle(trashPo.getProjectId(), trashPo.getObjectId());
            if (res) {
                removeFromRecycleBin(trashId);
            }
            return res;
        } catch (RecoverException e) {
            throw new SwValidationException(ValidSubject.TRASH, e.getMessage(), e);
        }
    }

    public Boolean deleteTrash(String projectUrl, Long trashId) {
        TrashPo trashPo = trashMapper.find(trashId);
        if (trashPo == null) {
            throw new SwValidationException(ValidSubject.TRASH, "Can not find trash.");
        }
        if (!trashPo.getProjectId().equals(projectService.getProjectId(projectUrl))) {
            throw new SwValidationException(ValidSubject.TRASH, "Project is not match.");
        }
        return trashMapper.delete(trashId) > 0;
    }

    @Transactional
    public Long moveToRecycleBin(Trash trash, User operator) {
        BundleEntity bundle = getBundleAccessor(trash.getType()).findById(trash.getObjectId());
        Date retention = new Date();
        TrashPo po = TrashPo.builder()
                .projectId(trash.getProjectId())
                .objectId(bundle.getId())
                .operatorId(operator.getId())
                .size(0L)
                .trashType(trash.getType().name())
                .trashName(bundle.getName())
                .updatedTime(bundle.getModifiedTime())
                .retention(retention)
                .build();
        trashMapper.insert(po);
        return po.getId();
    }

    private void removeFromRecycleBin(Long trashId) {
        trashMapper.delete(trashId);
    }

    public BundleAccessor getBundleAccessor(Trash.Type type) {
        return (BundleAccessor) getAccessor(type);
    }

    private RecoverAccessor getRecoverAccessor(Trash.Type type) {
        return (RecoverAccessor) getAccessor(type);
    }

    private Object getAccessor(Type type) {
        switch (Optional.of(type)
                .orElseThrow(() -> new SwValidationException(ValidSubject.TRASH, "Trash type is null."))) {
            case MODEL:
                return modelDao;
            case DATASET:
                return datasetDao;
            case RUNTIME:
                return runtimeDao;
            case EVALUATION:
                return jobDao;
            case REPORT:
                return reportDao;
            default:
                throw new SwValidationException(ValidSubject.TRASH, "Unknown trash type" + type);
        }
    }

    private TrashVo toTrashVo(TrashPo trashPo) {
        User operator = userService.loadUserById(trashPo.getOperatorId());
        if (operator == null) {
            throw new SwProcessException(ErrorType.DB, "Can not find operator. " + trashPo.getOperatorId());
        }
        return TrashVo.builder()
                .id(idConvertor.convert(trashPo.getId()))
                .type(trashPo.getTrashType())
                .name(trashPo.getTrashName())
                .size(trashPo.getSize())
                .trashedBy(operator.getName())
                .lastUpdatedTime(trashPo.getUpdatedTime().getTime())
                .trashedTime(trashPo.getCreatedTime().getTime())
                .retentionTime(trashPo.getRetention().getTime())
                .build();
    }
}
