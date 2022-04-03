/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swmp;

import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageInfoVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVO;
import ai.starwhale.mlops.api.protocol.swmp.SWModelPackageVersionVO;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.swmp.SWMPObject.Version;
import com.github.pagehelper.PageHelper;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SWModelPackageService {

    @Resource
    private SWModelPackageMapper swmpMapper;

    @Resource
    private SWModelPackageVersionMapper swmpVersionMapper;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private SWMPConvertor swmpConvertor;

    @Resource
    private SWMPVersionConvertor versionConvertor;

    public List<SWModelPackageVO> listSWMP(SWMPObject swmp, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWModelPackageEntity> entities = swmpMapper.listSWModelPackages(
            idConvertor.revert(swmp.getProjectId()), swmp.getName());

        return entities.stream()
            .map(swmpConvertor::convert)
            .collect(Collectors.toList());
    }

    public Boolean deleteSWMP(SWMPObject swmp) {
        int res = swmpMapper.deleteSWModelPackage(idConvertor.revert(swmp.getId()));
        return res > 0;
    }

    public SWModelPackageInfoVO getSWMPInfo(SWMPObject swmp) {
        Long modelID = idConvertor.revert(swmp.getId());
        SWModelPackageEntity model = swmpMapper.findSWModelPackageById(modelID);
        SWModelPackageVersionEntity latestVersion = swmpVersionMapper.getLatestVersion(modelID);
        String meta = latestVersion.getVersionMeta();

        return SWModelPackageInfoVO.builder().modelName(model.getSwmpName())
            .files(List.of()) // todo(dreamlandliu) parse file info in meta
            .build();
    }

    public Boolean modifySWMPVersion(Version version) {
        int update = swmpVersionMapper.update(
            SWModelPackageVersionEntity.builder()
                .id(idConvertor.revert(version.getId()))
                .versionTag(version.getTag())
                .storagePath(version.getStoragePath())
                .build());
        return update > 0;
    }

    public Boolean revertVersionTo(SWMPObject swmp) {
        int res = swmpVersionMapper.revertTo(idConvertor.revert(swmp.getLatestVersion().getId()));

        return res > 0;
    }

    public List<SWModelPackageVersionVO> listSWMPVersionHistory(SWMPObject swmp, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        List<SWModelPackageVersionEntity> entities = swmpVersionMapper.listVersions(
            idConvertor.revert(swmp.getId()), swmp.getLatestVersion().getName());

        return entities.stream()
            .map(versionConvertor::convert)
            .collect(Collectors.toList());
    }

    public String addSWMP(SWMPObject swmp) {
        SWModelPackageEntity entity = SWModelPackageEntity.builder()
            .swmpName(swmp.getName())
            .ownerId(idConvertor.revert(swmp.getOwnerId()))
            .projectId(idConvertor.revert(swmp.getProjectId()))
            .build();
        swmpMapper.addSWModelPackage(entity);
        return idConvertor.convert(entity.getId());
    }

    public String addVersion(SWMPObject swmp) {
        SWModelPackageVersionEntity entity = SWModelPackageVersionEntity.builder()
            .swmpId(idConvertor.revert(swmp.getId()))
            .ownerId(idConvertor.revert(swmp.getLatestVersion().getOwnerId()))
            .versionTag(swmp.getLatestVersion().getTag())
            .versionName(swmp.getLatestVersion().getName())
            .versionMeta(swmp.getLatestVersion().getMeta())
            .storagePath(swmp.getLatestVersion().getStoragePath())
            .build();
        swmpVersionMapper.addNewVersion(entity);
        return idConvertor.convert(entity.getId());
    }

    public SWModelPackageVO findModelByVersionId(String versionId) {
        SWModelPackageVersionEntity mv = swmpVersionMapper.getVersionById(
            idConvertor.revert(versionId));
        SWModelPackageEntity entity = swmpMapper.findSWModelPackageById(mv.getSwmpId());

        return swmpConvertor.convert(entity);
    }
}
