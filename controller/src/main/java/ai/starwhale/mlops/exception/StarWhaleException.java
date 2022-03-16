/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.exception;

/**
 * Star Whale business exception
 */
public abstract class StarWhaleException extends RuntimeException {

    /**
     * every exception should has a code to be exposed to user. the code shall be unique among all StarWhaleExceptions
     * @return user oriented error code
     */
    public abstract String getCode();

    /**
     * the exception message that is to be exposed to user
     * @return user oriented error message
     */
    public abstract String getTip();

}
