/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.exception.api;

import ai.starwhale.mlops.exception.StarWhaleException;
import org.springframework.http.HttpStatus;

public class StarWhaleApiException extends RuntimeException {

    StarWhaleException starWhaleException;
    HttpStatus httpStatus;
    public StarWhaleApiException(StarWhaleException starWhaleException,HttpStatus httpStatus) {
        this.starWhaleException = starWhaleException;
        this.httpStatus = httpStatus;
    }

    /**
     * every exception should has a code to be exposed to user. the code shall be unique among all StarWhaleExceptions
     * @return user oriented error code
     */
    public String getCode(){
        return starWhaleException.getCode();
    }

    /**
     * the exception message that is to be exposed to user
     * @return user oriented error message
     */
    public String getTip(){
        return starWhaleException.getTip();
    }

    /**
     * every api exception should has a HttpStatus to be exposed to user.
     * @return user oriented error code
     */
    public HttpStatus getHttpStatus(){
        return this.httpStatus;
    }
}
