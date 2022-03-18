/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.exception;

public class SWValidationException extends StarWhaleException {

    static final String PREFIX_CODE="400";

    static final String PREFIX_TIP="invalid request on subject ";

    private String code;
    private String tip;

    public SWValidationException(ValidSubject validSubject){
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

    public enum ValidSubject{
        JOB("001","JOB"),
        TASK("002","TASK"),
        USER("003","USER"),
        NODE("004","NODE"),
        SWDS("005","Star Whale Data Set"),
        SDMP("006","Star Whale Model Package");
        String code;
        String tipSubject;
        ValidSubject(String code,String tipSubject){
            this.code = code;
            this.tipSubject = tipSubject;
        }
    }
}
