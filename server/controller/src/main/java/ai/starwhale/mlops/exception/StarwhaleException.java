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

package ai.starwhale.mlops.exception;

/**
 * Starwhale business exception
 */
public abstract class StarwhaleException extends RuntimeException {

    protected StarwhaleException() {
        super();
    }

    protected StarwhaleException(String message) {
        super(message);
    }

    protected StarwhaleException(Throwable e) {
        super(e);
    }

    protected StarwhaleException(String message, Throwable e) {
        super(message, e);
    }

    /**
     * every exception should has a code to be exposed to user. the code shall be unique among all StarwhaleExceptions
     *
     * @return user oriented error code
     */
    public abstract String getCode();

    /**
     * the exception message that is to be exposed to user
     *
     * @return user oriented error message
     */
    public abstract String getTip();

}
