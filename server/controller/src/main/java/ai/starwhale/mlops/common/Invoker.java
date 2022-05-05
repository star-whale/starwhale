package ai.starwhale.mlops.common;

import ai.starwhale.mlops.exception.api.StarWhaleApiException;

public interface Invoker<T> {

    void invoke(T param) throws StarWhaleApiException;

}
