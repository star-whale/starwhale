package ai.starwhale.mlops.domain.bundle;

import ai.starwhale.mlops.domain.bundle.base.BundleEntity;

public interface BundleAccessor {

    BundleEntity findById(Long id);

    BundleEntity findByName(String name, Long projectId);
}
