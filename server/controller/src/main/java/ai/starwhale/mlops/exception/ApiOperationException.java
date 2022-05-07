/**
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
