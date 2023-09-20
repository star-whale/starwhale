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

package ai.starwhale.mlops.api.protocol;

public enum Code {
    success("Success"),
    validationException("ValidationException"),
    internalServerError("InternalServerError"),
    accessDenied("AccessDenied"),
    Unauthorized("Unauthorized"),
    unknownError("unknownError");
    private final String type;

    Code(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public <T> ResponseMessage<T> asResponse(T data) {
        return new ResponseMessage<>(this.name(), this.type, data);
    }

    public <T> NullableResponseMessage<T> asNullableResponse(T data) {
        return new NullableResponseMessage<>(this.name(), this.type, data);
    }
}
