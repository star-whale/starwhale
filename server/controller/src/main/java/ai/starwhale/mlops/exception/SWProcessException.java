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

public class SWProcessException extends StarWhaleException {

    static final String PREFIX_CODE="SYSTEM ERROR: ";

    static final String PREFIX_TIP="ERROR occurs while dealing with ";

    private final String code;
    private String tip;

    public SWProcessException(ErrorType errorType){
        super(PREFIX_TIP + errorType.tipSubject);
        this.code = PREFIX_CODE + errorType.tipSubject;
        this.tip = PREFIX_TIP + errorType.tipSubject;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getTip() {
        return this.tip;
    }

    public SWProcessException tip(String tip){
        this.tip += "\n";
        this.tip += tip;
        return this;
    }

    public enum ErrorType{
        STORAGE("001","STORAGE"),
        DB("002","DB"),
        NETWORK("003","NETWORK"),
        SYSTEM("004","SYSTEM");
        final String code;
        final String tipSubject;
        ErrorType(String code,String tipSubject){
            this.code = code;
            this.tipSubject = tipSubject;
        }
    }
}
