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

package ai.starwhale.mlops.domain.dataset;

import ai.starwhale.mlops.api.protocol.StorageFileVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetInfoVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVersionVo;
import ai.starwhale.mlops.api.protocol.dataset.DatasetVo;
import ai.starwhale.mlops.api.protocol.dataset.dataloader.DataIndexDesc;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.VersionAliasConvertor;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import ai.starwhale.mlops.domain.bundle.remove.RemoveManager;
import ai.starwhale.mlops.domain.bundle.revert.RevertManager;
import ai.starwhale.mlops.domain.bundle.tag.TagException;
import ai.starwhale.mlops.domain.bundle.tag.TagManager;
import ai.starwhale.mlops.domain.dataset.bo.DatasetQuery;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersionQuery;
import ai.starwhale.mlops.domain.dataset.converter.DatasetVersionConvertor;
import ai.starwhale.mlops.domain.dataset.converter.DatasetVoConvertor;
import ai.starwhale.mlops.domain.dataset.dataloader.DataLoader;
import ai.starwhale.mlops.domain.dataset.dataloader.DataReadRequest;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.objectstore.DsFileGetter;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.storage.StorageService;
import ai.starwhale.mlops.domain.trash.Trash;
import ai.starwhale.mlops.domain.trash.Trash.Type;
import ai.starwhale.mlops.domain.trash.TrashService;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class DatasetService {

    private final DatasetMapper datasetMapper;
    private final DatasetVersionMapper datasetVersionMapper;
    private final DatasetVoConvertor datasetVoConvertor;
    private final DatasetVersionConvertor versionConvertor;
    private final StorageService storageService;
    private final ProjectManager projectManager;
    private final DatasetManager datasetManager;
    private final IdConvertor idConvertor;
    private final VersionAliasConvertor versionAliasConvertor;
    private final UserService userService;
    private final DsFileGetter dsFileGetter;
    private final DataLoader dataLoader;
    private final TrashService trashService;
    @Setter
    private BundleManager bundleManager;

    public DatasetService(ProjectManager projectManager, DatasetMapper datasetMapper,
            DatasetVersionMapper datasetVersionMapper, DatasetVoConvertor datasetVoConvertor,
            DatasetVersionConvertor versionConvertor, StorageService storageService, DatasetManager datasetManager,
            IdConvertor idConvertor, VersionAliasConvertor versionAliasConvertor, UserService userService,
            DsFileGetter dsFileGetter, DataLoader dataLoader, TrashService trashService) {
        this.projectManager = projectManager;
        this.datasetMapper = datasetMapper;
        this.datasetVersionMapper = datasetVersionMapper;
        this.datasetVoConvertor = datasetVoConvertor;
        this.versionConvertor = versionConvertor;
        this.storageService = storageService;
        this.datasetManager = datasetManager;
        this.idConvertor = idConvertor;
        this.versionAliasConvertor = versionAliasConvertor;
        this.userService = userService;
        this.dsFileGetter = dsFileGetter;
        this.dataLoader = dataLoader;
        this.trashService = trashService;
        this.bundleManager = new BundleManager(
                idConvertor,
                versionAliasConvertor,
                projectManager,
                datasetManager,
                datasetManager
        );
    }


    public PageInfo<DatasetVo> listSwDataset(DatasetQuery query, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(query.getProjectUrl());
        List<DatasetEntity> entities = datasetMapper.listDatasets(projectId,
                query.getNamePrefix());

        return PageUtil.toPageInfo(entities, ds -> {
            DatasetVersionEntity version = datasetVersionMapper.getLatestVersion(ds.getId());
            DatasetVo vo = datasetVoConvertor.convert(ds);
            vo.setVersion(versionConvertor.convert(version));
            return vo;
        });
    }

    public Boolean deleteDataset(DatasetQuery query) {
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getDatasetUrl());
        Trash trash = Trash.builder()
                .projectId(projectManager.getProjectId(query.getProjectUrl()))
                .objectId(bundleManager.getBundleId(bundleUrl))
                .type(Type.DATASET)
                .build();
        trashService.moveToRecycleBin(trash, userService.currentUserDetail());
        return RemoveManager.create(bundleManager, datasetManager)
                .removeBundle(BundleUrl.create(query.getProjectUrl(), query.getDatasetUrl()));
    }

    public Boolean recoverDataset(String projectUrl, String datasetUrl) {
        throw new UnsupportedOperationException("Please use TrashService.recover() instead.");
    }

    public DatasetInfoVo getDatasetInfo(DatasetQuery query) {
        BundleUrl bundleUrl = BundleUrl.create(query.getProjectUrl(), query.getDatasetUrl());
        Long datasetId = bundleManager.getBundleId(bundleUrl);
        DatasetEntity ds = datasetMapper.findDatasetById(datasetId);
        if (ds == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.DATASET)
                    .tip("Unable to find dataset " + query.getDatasetUrl()), HttpStatus.BAD_REQUEST);
        }

        DatasetVersionEntity versionEntity = null;
        if (!StrUtil.isEmpty(query.getDatasetVersionUrl())) {
            Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                    .create(bundleUrl, query.getDatasetVersionUrl()), datasetId);
            versionEntity = datasetVersionMapper.getVersionById(versionId);
        }
        if (versionEntity == null) {
            versionEntity = datasetVersionMapper.getLatestVersion(ds.getId());
        }
        if (versionEntity == null) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.DATASET)
                    .tip("Unable to find the latest version of dataset " + query.getDatasetUrl()),
                    HttpStatus.BAD_REQUEST);
        }
        return toSwDatasetInfoVo(ds, versionEntity);

    }

    private DatasetInfoVo toSwDatasetInfoVo(DatasetEntity ds, DatasetVersionEntity versionEntity) {

        //Get file list in storage
        try {
            String storagePath = versionEntity.getStoragePath();
            List<StorageFileVo> collect = storageService.listStorageFile(storagePath);
            return DatasetInfoVo.builder()
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
            log.error("list dataset storage", e);
            throw new StarwhaleApiException(new SwProcessException(ErrorType.STORAGE)
                    .tip(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public Boolean modifyDatasetVersion(String projectUrl, String datasetUrl, String versionUrl,
            DatasetVersion version) {
        Long versionId = bundleManager.getBundleVersionId(BundleVersionUrl
                .create(projectUrl, datasetUrl, versionUrl));
        DatasetVersionEntity entity = DatasetVersionEntity.builder()
                .id(versionId)
                .versionTag(version.getTag())
                .build();
        int update = datasetVersionMapper.update(entity);
        log.info("Dataset Version has been modified. ID={}", entity.getId());
        return update > 0;
    }

    public Boolean manageVersionTag(String projectUrl, String datasetUrl, String versionUrl,
            TagAction tagAction) {

        try {
            return TagManager.create(bundleManager, datasetManager)
                    .updateTag(
                            BundleVersionUrl.create(projectUrl, datasetUrl, versionUrl),
                            tagAction);
        } catch (TagException e) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.DATASET).tip(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    public Boolean revertVersionTo(String projectUrl, String datasetUrl, String versionUrl) {
        return RevertManager.create(bundleManager, datasetManager)
                .revertVersionTo(BundleVersionUrl.create(projectUrl, datasetUrl, versionUrl));
    }

    public PageInfo<DatasetVersionVo> listDatasetVersionHistory(DatasetVersionQuery query, PageParams pageParams) {
        Long datasetId = bundleManager.getBundleId(BundleUrl.create(query.getProjectUrl(), query.getDatasetUrl()));
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<DatasetVersionEntity> entities = datasetVersionMapper.listVersions(
                datasetId, query.getVersionName(), query.getVersionTag());
        return PageUtil.toPageInfo(entities, versionConvertor::convert);
    }

    public List<DatasetVo> findDatasetsByVersionIds(List<Long> versionIds) {
        List<DatasetVersionEntity> versions = datasetVersionMapper.findVersionsByIds(versionIds);

        return versions.stream().map(version -> {
            DatasetEntity ds = datasetMapper.findDatasetById(version.getDatasetId());
            DatasetVo vo = datasetVoConvertor.convert(ds);
            vo.setVersion(versionConvertor.convert(version));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<DatasetInfoVo> listDs(String project, String name) {
        if (StringUtils.hasText(name)) {
            Long projectId = projectManager.getProjectId(project);
            DatasetEntity ds = datasetMapper.findByName(name, projectId);
            if (null == ds) {
                throw new SwValidationException(ValidSubject.DATASET)
                        .tip("Unable to find the dataset with name " + name);
            }
            return swDatasetInfoOfDs(ds);
        }
        ProjectEntity projectEntity = projectManager.findByNameOrDefault(project,
                userService.currentUserDetail().getIdTableKey());

        List<DatasetEntity> swDatasetEntities = datasetMapper.listDatasets(projectEntity.getId(), null);
        if (null == swDatasetEntities || swDatasetEntities.isEmpty()) {
            return List.of();
        }
        return swDatasetEntities.parallelStream()
                .map(this::swDatasetInfoOfDs)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<DatasetInfoVo> swDatasetInfoOfDs(DatasetEntity ds) {
        List<DatasetVersionEntity> swDatasetVersionEntities = datasetVersionMapper.listVersions(
                ds.getId(), null, null);
        if (null == swDatasetVersionEntities || swDatasetVersionEntities.isEmpty()) {
            return List.of();
        }
        return swDatasetVersionEntities.parallelStream()
                .map(entity -> toSwDatasetInfoVo(ds, entity)).collect(Collectors.toList());
    }

    public DatasetVersionEntity query(String projectUrl, String datasetUrl, String versionUrl) {
        Long projectId = projectManager.getProjectId(projectUrl);
        DatasetEntity entity = datasetMapper.findByName(datasetUrl, projectId);
        if (null == entity) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.DATASET), HttpStatus.NOT_FOUND);
        }
        DatasetVersionEntity versionEntity = datasetVersionMapper.findByDsIdAndVersionName(entity.getId(), versionUrl);
        if (null == versionEntity) {
            throw new StarwhaleApiException(new SwValidationException(ValidSubject.DATASET), HttpStatus.NOT_FOUND);
        }
        return versionEntity;
    }

    public DataIndexDesc nextData(DataReadRequest request) {
        var dataRange = dataLoader.next(request);
        return Objects.isNull(dataRange) ? null : DataIndexDesc.builder()
                .start(dataRange.getStart())
                .end(dataRange.getEnd())
                .build();
    }

    public byte[] dataOf(Long datasetId, String uri, String authName, String offset,
            String size) {
        return dsFileGetter.dataOf(datasetId, uri, authName, offset, size);
    }

    public String signLink(Long id, String uri, String authName, Long expTimeMillis) {
        return dsFileGetter.linkOf(id, uri, authName, expTimeMillis);
    }
}
