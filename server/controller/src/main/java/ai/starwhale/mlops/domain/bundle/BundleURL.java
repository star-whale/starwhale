package ai.starwhale.mlops.domain.bundle;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BundleURL {

    private String projectUrl;

    private String bundleUrl;
}
