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

public class SWAuthException extends StarWhaleException {

    static final String PREFIX_CODE="401";

    static final String PREFIX_TIP="you have no permission to do ";

    private final String code;
    private String tip;

    public SWAuthException(AuthType authType){
        super(PREFIX_TIP + authType.tipSubject);
        this.code = PREFIX_CODE + authType.code;
        this.tip = PREFIX_TIP + authType.tipSubject;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getTip() {
        return this.tip;
    }

    public SWAuthException tip(String tip){
        this.tip += "\n";
        this.tip += tip;
        return this;
    }

    public enum AuthType {
        SWDS_UPLOAD("001","SWDS UPLOAD"),
        SWMP_UPLOAD("002","SWMP UPLOAD"),
        CURRENT_USER("003","CURRENT USER");
        final String code;
        final String tipSubject;
        AuthType(String code, String tipSubject){
            this.code = code;
            this.tipSubject = tipSubject;
        }
    }
}
