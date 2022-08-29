package ai.starwhale.mlops.domain.bundle;

import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;

public interface BundleVersionAccessor {

    BundleVersionEntity findVersionById(Long bundleVersionId);
    BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId);
}
