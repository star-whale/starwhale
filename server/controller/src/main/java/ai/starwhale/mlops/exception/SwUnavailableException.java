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

public class SwUnavailableException extends StarwhaleException {

    private static final String PREFIX_CODE = "Service is Unavailable: ";
    private static final String PREFIX_TIP = "Service is Unavailable ";

    private final String code;
    private final String tip;

    public SwUnavailableException(Reason reason, String tip) {
        super(PREFIX_TIP + reason.tipSubject + "\n" + tip);
        this.code = PREFIX_CODE + reason.code;
        this.tip = PREFIX_TIP + reason.tipSubject + "\n" + tip;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getTip() {
        return tip;
    }

    public enum Reason {
        UPGRADING("001", "Upgrading");
        final String code;
        final String tipSubject;

        Reason(String code, String tipSubject) {
            this.code = code;
            this.tipSubject = tipSubject;
        }

        public String getCode() {
            return code;
        }

        public String getTipSubject() {
            return tipSubject;
        }
    }
}
