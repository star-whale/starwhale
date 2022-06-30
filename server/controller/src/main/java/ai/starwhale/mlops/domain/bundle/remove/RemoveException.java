package ai.starwhale.mlops.domain.bundle.remove;

import ai.starwhale.mlops.exception.StarWhaleException;

public class RemoveException extends StarWhaleException {

    public RemoveException(String message) {
        super(message);
    }

    @Override
    public String getCode() {
        return "REMOVE";
    }

    @Override
    public String getTip() {
        return getMessage();
    }
}
