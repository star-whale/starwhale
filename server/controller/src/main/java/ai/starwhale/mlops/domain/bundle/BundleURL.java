package ai.starwhale.mlops.domain.bundle;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BundleURL {

    private String projectUrl;

    private String bundleUrl;

    public static BundleURL create(String projectUrl, String bundleUrl) {
        return new BundleURL(projectUrl, bundleUrl);
    }
}
