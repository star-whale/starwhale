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
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.OrderParams;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.recover.RecoverAccessor;
import ai.starwhale.mlops.domain.bundle.recover.RecoverException;
import ai.starwhale.mlops.domain.bundle.recover.RecoverManager;
import ai.starwhale.mlops.domain.dataset.DatasetManager;
import ai.starwhale.mlops.domain.job.JobManager;
import ai.starwhale.mlops.domain.model.ModelManager;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.runtime.RuntimeManager;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.bo.TrashQuery;
import ai.starwhale.mlops.domain.trash.mapper.TrashMapper;
import ai.starwhale.mlops.domain.trash.po.TrashPo;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.domain.user.mapper.UserMapper;
import ai.starwhale.mlops.domain.user.po.UserEntity;
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
    private final UserMapper userMapper;
    private final ProjectManager projectManager;
    private final ModelManager modelManager;
    private final DatasetManager datasetManager;
    private final RuntimeManager runtimeManager;
    private final JobManager jobManager;
    private final IdConvertor idConvertor;

    public TrashService(TrashMapper trashMapper, UserMapper userMapper, ProjectManager projectManager,
            ModelManager modelManager, DatasetManager datasetManager,
            RuntimeManager runtimeManager, JobManager jobManager, IdConvertor idConvertor) {
        this.trashMapper = trashMapper;
        this.userMapper = userMapper;
        this.projectManager = projectManager;
        this.modelManager = modelManager;
        this.datasetManager = datasetManager;
        this.runtimeManager = runtimeManager;
        this.jobManager = jobManager;
        this.idConvertor = idConvertor;
    }

    public PageInfo<TrashVo> listTrash(TrashQuery query, PageParams pageParams, OrderParams orderParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(query.getProjectUrl());
        Long operatorId = null;
        if (StrUtil.isNotEmpty(query.getOperator())) {
            if (idConvertor.isId(query.getOperator())) {
                operatorId = idConvertor.revert(query.getOperator());
            } else {
                UserEntity user = userMapper.findUserByName(query.getOperator());
                if (user != null) {
                    operatorId = user.getId();
                }
            }
        }
        List<TrashPo> poList = trashMapper.list(projectId, operatorId, query.getName(), query.getType());
        return PageUtil.toPageInfo(poList, this::toTrashVo);
    }

    @Transactional
    public Boolean recover(String projectUrl, Long trashId) {
        TrashPo trashPo = trashMapper.find(trashId);
        if (!trashPo.getProjectId().equals(projectManager.getProjectId(projectUrl))) {
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
            throw new SwValidationException(ValidSubject.TRASH, e.getMessage());
        }
    }

    @Transactional
    public Long moveToRecycleBin(Trash trash, User operator) {
        BundleEntity bundle = getBundleAccessor(trash.getType()).findById(trash.getObjectId());
        Date retention = new Date();
        TrashPo po = TrashPo.builder()
                .projectId(trash.getProjectId())
                .objectId(bundle.getId())
                .operatorId(operator.getId())
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

    private BundleAccessor getBundleAccessor(Trash.Type type) {
        return (BundleAccessor) getAccessor(type);
    }

    private RecoverAccessor getRecoverAccessor(Trash.Type type) {
        return (RecoverAccessor) getAccessor(type);
    }

    private Object getAccessor(Type type) {
        switch (Optional.of(type)
                .orElseThrow(() -> new SwValidationException(ValidSubject.TRASH, "Trash type is null."))) {
            case MODEL:
                return modelManager;
            case DATASET:
                return datasetManager;
            case RUNTIME:
                return runtimeManager;
            case EVALUATION:
                return jobManager;
            default:
                throw new SwValidationException(ValidSubject.TRASH, "Unknown trash type" + type);
        }
    }

    private TrashVo toTrashVo(TrashPo trashPo) {
        UserEntity operator = userMapper.findUser(trashPo.getOperatorId());
        if (operator == null) {
            throw new SwProcessException(ErrorType.DB)
                    .tip("Can not find operator. " + trashPo.getOperatorId());
        }
        return TrashVo.builder()
                .id(idConvertor.convert(trashPo.getId()))
                .type(trashPo.getTrashType())
                .name(trashPo.getTrashName())
                .size(trashPo.getSize())
                .trashedBy(operator.getUserName())
                .lastUpdatedTime(trashPo.getUpdatedTime().getTime())
                .trashedTime(trashPo.getCreatedTime().getTime())
                .retentionTime(trashPo.getRetention().getTime())
                .build();
    }
}
