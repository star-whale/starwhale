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

package ai.starwhale.mlops.domain.swds;

import ai.starwhale.mlops.api.protocol.StorageFileVo;
import ai.starwhale.mlops.api.protocol.swds.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.swds.DatasetVo;
import ai.starwhale.mlops.api.protocol.swds.SwDatasetInfoVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.recover.RecoverException;
import ai.starwhale.mlops.domain.bundle.recover.RecoverManager;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.TagException;
import ai.starwhale.mlops.domain.bundle.tag.TagManager;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.swds.bo.SwdsQuery;
import ai.starwhale.mlops.domain.swds.bo.SwdsVersion;
import ai.starwhale.mlops.domain.swds.bo.SwdsVersionQuery;
import ai.starwhale.mlops.domain.swds.converter.SwdsVersionConvertor;
import ai.starwhale.mlops.domain.swds.converter.SwdsVoConvertor;
import ai.starwhale.mlops.domain.swds.mapper.SwDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SwDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.objectstore.DsFileGetter;
import ai.starwhale.mlops.domain.swds.po.SwDatasetEntity;
import ai.starwhale.mlops.domain.swds.po.SwDatasetVersionEntity;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.configuration.StorageProperties;
import ai.starwhale.mlops.storage.env.StorageEnv;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class SwDatasetService {

    @Resource
    private SwDatasetMapper swdsMapper;

    @Resource
    private SwDatasetVersionMapper swdsVersionMapper;

    @Resource
    private SwdsVoConvertor swdsVoConvertor;

    @Resource
    private SwdsVersionConvertor versionConvertor;

    @Resource
    private StorageService storageService;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private SwdsManager swdsManager;

    @Resource
    private IdConvertor idConvertor;

    @Resource
    private VersionAliasConvertor versionAliasConvertor;

    @Resource
    private UserService userService;

    @Resource
    private DsFileGetter dsFileGetter;

    private BundleManager bundleManager() {
        return new BundleManager(idConvertor, versionAliasConvertor, projectManager, swdsManager, swdsManager,
                ValidSubject.SWDS);
    }

    public PageInfo<DatasetVo> listSwDataset(SwdsQuery query, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(query.getProjectUrl());
        List<SwDatasetEntity> entities = swdsMapper.listDatasets(projectId,
                query.getNamePrefix());

        return PageUtil.toPageInfo(entities, ds -> {
            SwDatasetVersionEntity version = swdsVersionMapper.getLatestVersion(ds.getId());
            DatasetVo vo = swdsVoConvertor.convert(ds);
            vo.setVersion(versionConvertor.convert(version));
            return vo;
        });
    }

    public Boolean deleteSwds(SwdsQuery query) {
        return RemoveManager.create(bundleManager(), swdsManager)
                .removeBundle(BundleUrl.create(query.getProjectUrl(), query.getSwdsUrl()));
    }

    public Boolean recoverSwds(String projectUrl, String datasetUrl) {
        try {
            return RecoverManager.create(projectManager, swdsManager, idConvertor)
                    .recoverBundle(BundleUrl.create(projectUrl, datasetUrl));
        } catch (RecoverException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS).tip(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    public SwDatasetInfoVo getSwdsInfo(SwdsQuery query) {
        BundleManager bundleManager = bundleManager();
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getSwdsUrl());
        Long datasetId = bundleManager.getBundleId(bundleUrl);
        SwDatasetEntity ds = swdsMapper.findDatasetById(datasetId);
        if (ds == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS)
                    .tip("Unable to find swds " + query.getSwdsUrl()), HttpStatus.BAD_REQUEST);
        }

        SwDatasetVersionEntity versionEntity = null;
        if (!StrUtil.isEmpty(query.getSwdsVersionUrl())) {
            Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                    .create(bundleUrl, query.getSwdsVersionUrl()), datasetId);
            versionEntity = swdsVersionMapper.getVersionById(versionId);
        }
        if (versionEntity == null) {
            versionEntity = swdsVersionMapper.getLatestVersion(ds.getId());
        }
        if (versionEntity == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS)
                    .tip("Unable to find the latest version of swds " + query.getSwdsUrl()), HttpStatus.BAD_REQUEST);
        }
        return toSwDatasetInfoVo(ds, versionEntity);

    }

    private SwDatasetInfoVo toSwDatasetInfoVo(SwDatasetEntity ds, SwDatasetVersionEntity versionEntity) {

        //Get file list in storage
        try {
            String storagePath = versionEntity.getStoragePath();
            List<StorageFileVo> collect = storageService.listStorageFile(storagePath);
            return SwDatasetInfoVo.builder()
                    .id(idConvertor.convert(ds.getId()))
                    .name(ds.getDatasetName())
                    .versionName(versionEntity.getVersionName())
                    .versionAlias(versionAliasConvertor.convert(versionEntity.getVersionOrder()))
                    .versionTag(versionEntity.getVersionTag())
                    .versionMeta(versionEntity.getVersionMeta())
                    .createdTime(versionEntity.getCreatedTime().getTime())
                    .indexTable(versionEntity.getIndexTable())
                    .files(collect)
                    .build();

        } catch (IOException e) {
            log.error("list swds storage", e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE)
                    .tip(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public Boolean modifySwdsVersion(String projectUrl, String swdsUrl, String versionUrl, SwdsVersion version) {
        Long versionId = bundleManager().getBundleVersionId(BundleVersionUrl
                .create(projectUrl, swdsUrl, versionUrl));
        SwDatasetVersionEntity entity = SwDatasetVersionEntity.builder()
                .id(versionId)
                .versionTag(version.getTag())
                .build();
        int update = swdsVersionMapper.update(entity);
        log.info("SWDS Version has been modified. ID={}", entity.getId());
        return update > 0;
    }

    public Boolean manageVersionTag(String projectUrl, String datasetUrl, String versionUrl,
            TagAction tagAction) {

        try {
            return TagManager.create(bundleManager(), swdsManager)
                    .updateTag(
                            BundleVersionUrl.create(projectUrl, datasetUrl, versionUrl),
                            tagAction);
        } catch (TagException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS).tip(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    public Boolean revertVersionTo(String projectUrl, String swdsUrl, String versionUrl) {
        return RevertManager.create(bundleManager(), swdsManager)
                .revertVersionTo(BundleVersionUrl.create(projectUrl, swdsUrl, versionUrl));
    }

    public PageInfo<DatasetVersionVo> listDatasetVersionHistory(SwdsVersionQuery query, PageParams pageParams) {
        Long swdsId = bundleManager().getBundleId(BundleUrl.create(query.getProjectUrl(), query.getSwdsUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SwDatasetVersionEntity> entities = swdsVersionMapper.listVersions(
                swdsId, query.getVersionName(), query.getVersionTag());
        return PageUtil.toPageInfo(entities, versionConvertor::convert);
    }

    public List<DatasetVo> findDatasetsByVersionIds(List<Long> versionIds) {
        List<SwDatasetVersionEntity> versions = swdsVersionMapper.findVersionsByIds(versionIds);

        return versions.stream().map(version -> {
            SwDatasetEntity ds = swdsMapper.findDatasetById(version.getDatasetId());
            DatasetVo vo = swdsVoConvertor.convert(ds);
            vo.setVersion(versionConvertor.convert(version));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<SwDatasetInfoVo> listDs(String project, String name) {
        if (StringUtils.hasText(name)) {
            Long projectId = projectManager.getProjectId(project);
            SwDatasetEntity ds = swdsMapper.findByName(name, projectId);
            if (null == ds) {
                throw new SwValidationException(ValidSubject.SWDS)
                        .tip("Unable to find the swds with name " + name);
            }
            return swDatasetInfoOfDs(ds);
        }
        ProjectEntity projectEntity = projectManager.findByNameOrDefault(project,
                userService.currentUserDetail().getIdTableKey());

        List<SwDatasetEntity> swDatasetEntities = swdsMapper.listDatasets(projectEntity.getId(), null);
        if (null == swDatasetEntities || swDatasetEntities.isEmpty()) {
            return List.of();
        }
        return swDatasetEntities.parallelStream()
                .map(this::swDatasetInfoOfDs)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<SwDatasetInfoVo> swDatasetInfoOfDs(SwDatasetEntity ds) {
        List<SwDatasetVersionEntity> swDatasetVersionEntities = swdsVersionMapper.listVersions(
                ds.getId(), null, null);
        if (null == swDatasetVersionEntities || swDatasetVersionEntities.isEmpty()) {
            return List.of();
        }
        return swDatasetVersionEntities.parallelStream()
                .map(entity -> toSwDatasetInfoVo(ds, entity)).collect(Collectors.toList());
    }

    public SwDatasetVersionEntity query(String projectUrl, String datasetUrl, String versionUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        SwDatasetEntity entity = swdsMapper.findByName(datasetUrl, projectId);
        if (null == entity) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS), HttpStatus.NOT_FOUND);
        }
        SwDatasetVersionEntity versionEntity = swdsVersionMapper.findByDsIdAndVersionName(entity.getId(), versionUrl);
        if (null == versionEntity) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.SWDS), HttpStatus.NOT_FOUND);
        }
        return versionEntity;
    }

    public byte[] dataOf(Long datasetId, String uri, String authName, String offset,
            String size) {
        return dsFileGetter.dataOf(datasetId, uri, authName, offset, size);
    }
}
