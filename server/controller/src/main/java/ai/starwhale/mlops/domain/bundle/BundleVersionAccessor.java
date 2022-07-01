package ai.starwhale.mlops.domain.bundle;

import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;

public interface BundleVersionAccessor {

    BundleVersionEntity findVersionByNameAndBundleId(String name, Long bundleId);
}
