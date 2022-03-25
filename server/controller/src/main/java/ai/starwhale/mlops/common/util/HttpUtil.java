/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.common.util;

import ai.starwhale.mlops.api.protocol.Code;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

public class HttpUtil {

    public static void error(HttpServletResponse response, int httpStatus, Code code, String message)
        throws IOException {
        response.resetBuffer();
        response.setStatus(httpStatus);
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        response.getOutputStream()
            .print(JSONUtil.createObj()
                .append("code", code.getType())
                .append("message", message)
                .toStringPretty());
        response.flushBuffer();
    }
}
