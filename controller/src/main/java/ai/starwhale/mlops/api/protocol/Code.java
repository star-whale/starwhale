/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.api.protocol;

import ai.starwhale.mlops.api.protocol.ResponseMessage;

public enum Code {
    success("Success"),
    validationException("ValidationException"),
    internalServerError("InternalServerError"),
    accessDenied("AccessDenied"),
    unknownError("unknownError");
    private final String type;
    Code(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }

    public <T> ResponseMessage<T> asResponse(T data) {
        return new ResponseMessage<>(this, this.type, data);
    }

    public void exception() {

    }
    class BaseException {

    }
}
