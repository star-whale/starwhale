/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.api.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ResponseMessage<T> {
    private static final String requestId = null;

    /**
     * 默认的成功码
     */
    public static final Code CODE_SUCCESS = Code.success;

    private Code code;
    private String message;
    private T data;

    public ResponseMessage(Code code, String message, T data) {
        setCode(code);
        setMessage(message);
        setData(data);
    }

    public ResponseMessage(Code code, String message) {
        setCode(code);
        setMessage(message);
    }

    public static <T> ResponseMessage<T> asSuccess(T data) {
        // todo 待确定如何国际化
        return new ResponseMessage<T>(CODE_SUCCESS, "", data);
    }
}
