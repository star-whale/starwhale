package ai.starwhale.mlops.domain.bundle.remove;

import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleURL;

public class RemoveManager {

    private final BundleManager bundleManager;

    private final RemoveAccessor removeAccessor;

    public static RemoveManager create(BundleManager bundleManager, RemoveAccessor removeAccessor) {
        return new RemoveManager(bundleManager, removeAccessor);
    }

    private RemoveManager(BundleManager bundleManager, RemoveAccessor removeAccessor) {
        this.bundleManager = bundleManager;
        this.removeAccessor = removeAccessor;
    }

    public Boolean removeBundle(BundleURL bundleURL) {
        Long id = bundleManager.getBundleId(bundleURL);
        return removeAccessor.remove(id);
    }
}
