package ai.starwhale.mlops.domain.bundle;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BundleVersionURL {

    private String projectUrl;

    private String bundleUrl;

    private String versionUrl;
}
