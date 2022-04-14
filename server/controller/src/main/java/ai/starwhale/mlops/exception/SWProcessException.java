/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.exception;

public class SWProcessException extends StarWhaleException {

    static final String PREFIX_CODE="500";

    static final String PREFIX_TIP="ERROR occurs while dealing with ";

    private final String code;
    private String tip;

    public SWProcessException(ErrorType errorType){
        super(PREFIX_TIP + errorType.tipSubject);
        this.code = PREFIX_CODE + errorType.code;
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
        NETWORK("003","NETWORK");
        final String code;
        final String tipSubject;
        ErrorType(String code,String tipSubject){
            this.code = code;
            this.tipSubject = tipSubject;
        }
    }
}
