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

package ai.starwhale.test.login;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import org.junit.jupiter.api.Test;

public class TestLoginRequest {

    @Test
    public void login() {
        for (int i = 0; i < 5; i++) {
            HttpResponse httpResponse = HttpUtil.createPost("http://127.0.0.1:8082/api/v1/login")
                .body("{\"userName\": \"starwhale\", \"userPwd\": \"abcd1234\" }", "application/json")
                .executeAsync();

            System.out.println(httpResponse.body());
        }

        for (int i = 0; i < 3; i++) {
            HttpResponse httpResponse = HttpUtil.createPost("http://127.0.0.1:8082/api/v1/login")
                .body("{\"userName\": \"dsfa\", \"userPwd\": \"abcd1234\" }", "application/json")
                .executeAsync();

            System.out.println(httpResponse.body());
        }
    }
}
