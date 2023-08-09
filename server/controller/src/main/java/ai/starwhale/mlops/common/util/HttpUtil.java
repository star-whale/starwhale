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

package ai.starwhale.mlops.common.util;

import ai.starwhale.mlops.api.protocol.Code;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

public class HttpUtil {

    public static void error(HttpServletResponse response, int httpStatus, Code code, String message)
            throws IOException {
        response.resetBuffer();
        response.setStatus(httpStatus);
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream()
                .print(JSONUtil.createObj()
                        .append("code", code.getType())
                        .append("message", message)
                        .toStringPretty());
        response.flushBuffer();
    }

    public enum Resources {
        PROJECT, MODEL, DATASET, RUNTIME;

        public String getUrlTypeName() {
            return this.name().toLowerCase();
        }
    }

    public static String getResourceUrlFromPath(String path, Resources resourceType) {
        return getResourceUrlFromPath(path, resourceType.getUrlTypeName());
    }

    public static String getResourceUrlFromPath(String path, String resourceName) {
        String prefix = "/" + resourceName + "/";
        String regex = "(" + prefix + ")([^/?]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            return matcher.group().substring(prefix.length());
        } else {
            return null;
        }
    }
}
