/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.exception;

import static ai.starwhale.mlops.api.protocol.Code.internalServerError;

public class ApiOperationException extends StarWhaleException{

    public ApiOperationException() {
        super();
    }

    public ApiOperationException(String message) {
        super(message);
    }

    public ApiOperationException(String message, Throwable e) {
        super(message, e);
    }

    @Override
    public String getCode() {
        return internalServerError.getType();
    }

    @Override
    public String getTip() {
        return "Api operation error.";
    }
}
