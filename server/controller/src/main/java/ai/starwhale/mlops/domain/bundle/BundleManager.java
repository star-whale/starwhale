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
        if(idConvertor.isID(bundleUrl)) {
            return idConvertor.revert(bundleUrl);
        }

        Long projectId = projectAccessor.getProjectId(projectUrl);
        BundleEntity entity = bundleAccessor.findByName(bundleUrl, projectId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(validSubject)
                .tip(String.format("Unable to find %s %s", validSubject.name(), bundleUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }

    private Long getBundleVersionId(String versionUrl, Long bundleId) {
        if(idConvertor.isID(versionUrl)) {
            return idConvertor.revert(versionUrl);
        }
        BundleVersionEntity entity = bundleVersionAccessor.findVersionByNameAndBundleId(versionUrl, bundleId);
        if(entity == null) {
            throw new StarWhaleApiException(new SWValidationException(validSubject)
                .tip(String.format("Unable to find %s %s", validSubject.name(), versionUrl)), HttpStatus.BAD_REQUEST);
        }
        return entity.getId();
    }
}
