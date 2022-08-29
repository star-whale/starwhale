package ai.starwhale.mlops.domain.bundle;

import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.project.ProjectAccessor;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class BundleManager {

    private final ProjectAccessor projectAccessor;
    private final IDConvertor idConvertor;
    private final BundleAccessor bundleAccessor;
    private final BundleVersionAccessor bundleVersionAccessor;
    private final ValidSubject validSubject;

    public BundleManager(IDConvertor idConvertor,
        ProjectAccessor projectAccessor,
        BundleAccessor bundleAccessor,
        BundleVersionAccessor bundleVersionAccessor,
        ValidSubject validSubject) {
        this.idConvertor = idConvertor;
        this.projectAccessor = projectAccessor;
        this.bundleAccessor = bundleAccessor;
        this.bundleVersionAccessor = bundleVersionAccessor;
        this.validSubject = validSubject;
    }

    public Long getBundleId(BundleURL bundleURL) {
        return getBundleId(bundleURL.getBundleUrl(), bundleURL.getProjectUrl());
    }

    public Long getBundleVersionId(BundleVersionURL bundleVersionURL) {
        Long bundleId = getBundleId(bundleVersionURL.getBundleURL());
        return getBundleVersionId(bundleVersionURL, bundleId);
    }
    public Long getBundleVersionId(BundleVersionURL bundleVersionURL, Long bundleId) {
        return getBundleVersionId(bundleVersionURL.getVersionUrl(), bundleId);
    }
    private Long getBundleId(String bundleUrl, String projectUrl) {
        BundleEntity entity;
        if(idConvertor.isID(bundleUrl)) {
            entity = bundleAccessor.findById(idConvertor.revert(bundleUrl));
        } else {
            Long projectId = projectAccessor.getProjectId(projectUrl);
            entity = bundleAccessor.findByName(bundleUrl, projectId);
        }
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(validSubject)
                .tip(String.format("Unable to find %s %s", validSubject.name(), bundleUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    private Long getBundleVersionId(String versionUrl, Long bundleId) {
        BundleVersionEntity entity;
        if(idConvertor.isID(versionUrl)) {
            entity = bundleVersionAccessor.findVersionById(idConvertor.revert(versionUrl));
        } else {
            entity = bundleVersionAccessor.findVersionByNameAndBundleId(versionUrl, bundleId);
        }
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(validSubject)
                .tip(String.format("Unable to find %s %s", validSubject.name(), versionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }
}
