package ai.starwhale.mlops.domain.bundle.tag;

import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.bundle.base.HasId;

public interface HasTag extends BundleEntity {

    void setTag(String tag);

    String getTag();
}
