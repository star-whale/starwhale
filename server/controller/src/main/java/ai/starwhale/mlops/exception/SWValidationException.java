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

public class SWValidationException extends StarWhaleException {

    static final String PREFIX_CODE="400";

    static final String PREFIX_TIP="invalid request on subject ";

    private final String code;
    private String tip;

    public SWValidationException(ValidSubject validSubject){
        super(PREFIX_TIP + validSubject.tipSubject);
        this.code = PREFIX_CODE + validSubject.code;
        this.tip = PREFIX_TIP + validSubject.tipSubject;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getTip() {
        return this.tip;
    }

    public SWValidationException tip(String tip){
        this.tip += "\n";
        this.tip += tip;
        return this;
    }

    public enum ValidSubject{
        JOB("001","JOB"),
        TASK("002","TASK"),
        USER("003","USER"),
        NODE("004","NODE"),
        SWDS("005","Star Whale Data Set"),
        SWMP("006","Star Whale Model Package"),
        PROJECT("007","PROJECT");
        final String code;
        final String tipSubject;
        ValidSubject(String code,String tipSubject){
            this.code = code;
            this.tipSubject = tipSubject;
        }
    }
}
