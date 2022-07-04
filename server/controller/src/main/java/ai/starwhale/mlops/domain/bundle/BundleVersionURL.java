package ai.starwhale.mlops.domain.bundle;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BundleVersionURL {

    private BundleURL bundleURL;

    private String versionUrl;


    public static BundleVersionURL create(BundleURL bundleURL, String versionUrl) {
        return new BundleVersionURL(bundleURL, versionUrl);
    }
    public static BundleVersionURL create(String projectUrl, String bundleUrl, String versionUrl) {
        return create(BundleURL.create(projectUrl, bundleUrl), versionUrl);
    }
}
