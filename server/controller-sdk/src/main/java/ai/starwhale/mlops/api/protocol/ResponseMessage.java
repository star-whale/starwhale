/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol;

import lombok.Data;

@Data
public class ResponseMessage<T> {
    // todo
    private String requestId = null;

    private String code;
    private String message;
    private T data;

    public ResponseMessage(String code, String message, T data) {
        setCode(code);
        setMessage(message);
        setData(data);
    }

    public ResponseMessage(String code, String message) {
        setCode(code);
        setMessage(message);
    }
}
