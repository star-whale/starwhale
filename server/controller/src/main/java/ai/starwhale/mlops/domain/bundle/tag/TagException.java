package ai.starwhale.mlops.domain.bundle.tag;

import ai.starwhale.mlops.exception.StarWhaleException;

public class TagException extends StarWhaleException {

    public TagException(String message) {
        super(message);
    }

    @Override
    public String getCode() {
        return "TAG";
    }

    @Override
    public String getTip() {
        return getMessage();
    }
}
