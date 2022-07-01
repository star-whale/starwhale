package ai.starwhale.mlops.domain.bundle.recover;

import ai.starwhale.mlops.exception.StarWhaleException;

public class RecoverException extends StarWhaleException {

    public RecoverException(String message) {
        super(message);
    }
    @Override
    public String getCode() {
        return "RECOVER";
    }

    @Override
    public String getTip() {
        return getMessage();
    }
}
