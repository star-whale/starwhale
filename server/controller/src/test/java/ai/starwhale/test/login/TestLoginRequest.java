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
