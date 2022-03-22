/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.configuration.security;

import static ai.starwhale.mlops.common.util.HttpUtil.error;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.domain.user.User;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = httpServletRequest.getHeader("Authentication");

        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            error(httpServletResponse, HttpStatus.FORBIDDEN.value(), Code.accessDenied, "Not logged in.");
            return;
        }

        String token = header.split(" ")[1].trim();

        if(!jwtTokenUtil.validate(token)) {
            error(httpServletResponse, HttpStatus.FORBIDDEN.value(), Code.accessDenied, "JWT token is expired or invalid.");
            return;
        }

        Claims claims = jwtTokenUtil.parseJWT(token);

        User user = BeanUtil.mapToBean(claims, User.class, true, CopyOptions.create());
        JwtLoginToken jwtLoginToken = new JwtLoginToken(user, "", user.getAuthorities());
        jwtLoginToken.setDetails(new WebAuthenticationDetails(httpServletRequest));
        SecurityContextHolder.getContext().setAuthentication(jwtLoginToken);
        filterChain.doFilter(httpServletRequest, httpServletResponse);

    }


}
