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

public class SwProcessException extends StarwhaleException {

    static final String PREFIX_CODE = "SYSTEM ERROR: ";

    static final String PREFIX_TIP = "ERROR occurs while dealing with ";

    private final String code;
    private final String tip;

    public SwProcessException(ErrorType errorType) {
        super(PREFIX_TIP + errorType.tipSubject);
        this.code = PREFIX_CODE + errorType.tipSubject;
        this.tip = PREFIX_TIP + errorType.tipSubject;
    }

    public SwProcessException(ErrorType errorType, String tip) {
        super(PREFIX_TIP + errorType.tipSubject + "\n" + tip);
        this.code = PREFIX_CODE + errorType.tipSubject;
        this.tip = PREFIX_TIP + errorType.tipSubject + "\n" + tip;
    }

    public SwProcessException(ErrorType errorType, String tip, Throwable cause) {
        super(PREFIX_TIP + errorType.tipSubject + "\n" + tip, cause);
        this.code = PREFIX_CODE + errorType.tipSubject;
        this.tip = PREFIX_TIP + errorType.tipSubject + "\n" + tip;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getTip() {
        return this.tip;
    }

    public enum ErrorType {
        STORAGE("001", "STORAGE"),
        DB("002", "DB"),
        NETWORK("003", "NETWORK"),
        SYSTEM("004", "SYSTEM"),
        INFRA("005", "INFRA"),
        DATASTORE("006", "DATASTORE"),
        K8S("007", "Kubernetes");
        final String code;
        final String tipSubject;

        ErrorType(String code, String tipSubject) {
            this.code = code;
            this.tipSubject = tipSubject;
        }
    }
}
