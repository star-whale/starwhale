/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.exception;

import org.springframework.http.HttpStatus;

public abstract class StarWhaleApiException extends StarWhaleException {

    protected StarWhaleApiException(String message) {
        super(message);
    }

    protected StarWhaleApiException(Throwable e) {
        super(e);
    }

    protected StarWhaleApiException(String message, Throwable e) {
        super(message, e);
    }

    /**
     * every api exception should has a HttpStatus to be exposed to user.
     * @return user oriented error code
     */
    public abstract HttpStatus getHttpStatus();
}
