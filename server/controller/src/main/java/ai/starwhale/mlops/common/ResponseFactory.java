/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.common;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;

public class ResponseFactory {
    /**
     * 默认的成功码
     */
    public static final Code CODE_SUCCESS = Code.success;

    public static <T> ResponseMessage<T> asSuccess(T data) {
        // todo 待确定如何国际化
        return new ResponseMessage<T>(CODE_SUCCESS.name(), "", data);
    }
}
