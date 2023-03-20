/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.exception.api;

import ai.starwhale.mlops.exception.StarwhaleException;
import org.springframework.http.HttpStatus;

public class StarwhaleApiException extends RuntimeException {

    StarwhaleException starwhaleException;
    HttpStatus httpStatus;

    public StarwhaleApiException(StarwhaleException starwhaleException, HttpStatus httpStatus) {
        super(starwhaleException.getTip());
        this.starwhaleException = starwhaleException;
        this.httpStatus = httpStatus;
    }

    /**
     * every exception should have a code to be exposed to user. the code shall be unique among all StarwhaleExceptions
     *
     * @return user oriented error code
     */
    public String getCode() {
        return starwhaleException.getCode();
    }

    /**
     * the exception message that is to be exposed to user
     *
     * @return user oriented error message
     */
    public String getTip() {
        return starwhaleException.getTip();
    }

    /**
     * every api exception should have a HttpStatus to be exposed to user.
     *
     * @return user oriented error code
     */
    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }
}
