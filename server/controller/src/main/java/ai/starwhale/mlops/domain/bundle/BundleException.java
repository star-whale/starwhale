package ai.starwhale.mlops.domain.bundle;

import ai.starwhale.mlops.exception.StarWhaleException;

public class BundleException extends StarWhaleException {

    public BundleException(String message) {
        super(message);
    }

    @Override
    public String getCode() {
        return "BUNDLE";
    }

    @Override
    public String getTip() {
        return getMessage();
    }
}
