package ai.starwhale.mlops.domain.bundle.recover;

import ai.starwhale.mlops.domain.bundle.BundleAccessor;
import ai.starwhale.mlops.domain.bundle.DeletedBundleAccessor;
import ai.starwhale.mlops.domain.bundle.base.HasId;
import java.util.List;

public interface RecoverAccessor extends BundleAccessor, DeletedBundleAccessor {
    Boolean recover(Long id);
}
