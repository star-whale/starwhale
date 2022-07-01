package ai.starwhale.mlops.domain.bundle.tag;

import ai.starwhale.mlops.common.TagAction;
import ai.starwhale.mlops.common.util.TagUtil;
import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleVersionURL;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class TagManager {

    private final BundleManager bundleManager;

    private final TagAccessor tagAccessor;

    public TagManager(BundleManager bundleManager, TagAccessor tagAccessor) {
        this.bundleManager = bundleManager;
        this.tagAccessor = tagAccessor;
    }

    public Boolean updateTag(BundleVersionURL bundleVersionURL, TagAction tagAction) throws TagException {
        Long id = bundleManager.getBundleId(bundleVersionURL.getBundleUrl(), bundleVersionURL.getProjectUrl());
        Long versionId = bundleManager.getBundleVersionId(bundleVersionURL.getVersionUrl(), id);

        HasTag entity = tagAccessor.findObjectWithTagById(versionId);
        if(entity == null) {
            throw new TagException(String.format("Unable to find the version, url=%s ", bundleVersionURL.getVersionUrl()));
        }
        entity.setTag(TagUtil.getTags(tagAction, entity.getTag()));
        int update = tagAccessor.updateTag(entity);
        log.info("Tag has been modified. ID={}", entity.getId());
        return update > 0;
    }
}
