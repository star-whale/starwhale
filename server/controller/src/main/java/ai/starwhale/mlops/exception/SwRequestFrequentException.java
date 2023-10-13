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

public class SwRequestFrequentException extends StarwhaleException {
    private static final String PREFIX_CODE = "Request Frequently: ";
    private static final String PREFIX_TIP = "Request Frequently: ";

    private final String code;
    private final String tip;

    public SwRequestFrequentException(RequestType requestType, String tip) {
        super(PREFIX_TIP + requestType.tipSubject + "\n" + tip);
        this.code = PREFIX_CODE + requestType.code;
        this.tip = PREFIX_TIP + requestType.tipSubject + "\n" + tip;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getTip() {
        return tip;
    }


    public enum RequestType {
        DATASET_LOAD("001", "Dataset load");
        final String code;
        final String tipSubject;

        RequestType(String code, String tipSubject) {
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
