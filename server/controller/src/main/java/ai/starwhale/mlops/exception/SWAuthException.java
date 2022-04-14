/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
