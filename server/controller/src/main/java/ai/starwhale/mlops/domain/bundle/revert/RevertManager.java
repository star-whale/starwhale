package ai.starwhale.mlops.domain.bundle.revert;

import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleURL;
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
        Long id = bundleManager.getBundleId(bundleVersionURL.getBundleURL());
        Long versionId = bundleManager.getBundleVersionId(bundleVersionURL, id);

        return revertAccessor.revertTo(id, versionId);
    }
}
