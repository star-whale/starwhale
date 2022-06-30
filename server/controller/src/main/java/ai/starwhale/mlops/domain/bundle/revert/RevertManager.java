package ai.starwhale.mlops.domain.bundle.revert;

import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleVersionURL;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RevertManager {

    private final BundleManager bundleManager;

    private final RevertAccessor revertAccessor;

    public static RevertManager create(BundleManager bundleManager, RevertAccessor revertAccessor) {
        return new RevertManager(bundleManager, revertAccessor);
    }
    private RevertManager(BundleManager bundleManager, RevertAccessor revertAccessor) {
        this.bundleManager = bundleManager;
        this.revertAccessor = revertAccessor;
    }

    public Boolean revertVersionTo(BundleVersionURL bundleVersionURL) {
        Long id = bundleManager.getBundleId(bundleVersionURL.getBundleUrl(), bundleVersionURL.getProjectUrl());
        Long versionId = bundleManager.getBundleVersionId(bundleVersionURL.getVersionUrl(), id);

        return revertAccessor.revertTo(id, versionId);
    }
}
