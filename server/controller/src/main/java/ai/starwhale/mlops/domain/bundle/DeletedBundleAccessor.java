package ai.starwhale.mlops.domain.bundle;

import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import java.util.List;

public interface DeletedBundleAccessor {

    BundleEntity findDeletedBundleById(Long id);

    List<? extends BundleEntity> listDeletedBundlesByName(String name, Long projectId);
}
