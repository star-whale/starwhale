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
