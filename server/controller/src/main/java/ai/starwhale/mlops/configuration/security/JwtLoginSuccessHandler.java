/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.configuration.security;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.user.User;
import ai.starwhale.mlops.domain.user.UserService;
import cn.hutool.json.JSONUtil;
import java.io.IOException;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class JwtLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Resource
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        User user = (User) authentication.getPrincipal();
        String jwtToken = jwtTokenUtil.generateAccessToken(user);
        UserVO vo = userService.findUserById(user.getId());

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", jwtToken));
        response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaders.AUTHORIZATION);
        response.setStatus(HttpStatus.OK.value());
        response.getWriter().write(JSONUtil.toJsonStr(Code.success.asResponse(vo)));
    }

}
