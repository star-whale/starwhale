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

package ai.starwhale.mlops.configuration.security;

import ai.starwhale.mlops.common.util.JwtTokenUtil;
import ai.starwhale.mlops.configuration.ControllerProperties;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.user.UserService;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

@Slf4j
@Configuration
@Order(3)
public class SecurityConfiguration2 extends WebSecurityConfigurerAdapter {
    @Resource
    private AdminServerProperties adminServer;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/").permitAll();
        // admin server
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
            new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminServer.path("/"));

        http.authorizeRequests(
                (authorizeRequests) -> authorizeRequests
                    .antMatchers(adminServer.path("/assets/**")).permitAll()
                    .antMatchers("/actuator/info").permitAll()
                    .antMatchers("/actuator/health").permitAll()
                    .antMatchers(adminServer.path("/login")).permitAll()
                    .anyRequest().authenticated())
            .formLogin(
                (formLogin) -> formLogin.loginPage(adminServer.path("/login")).successHandler(successHandler).and())
            .logout((logout) -> logout.logoutUrl(adminServer.path("/logout"))).httpBasic(Customizer.withDefaults())
            .csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher(adminServer.path("/instances"),
                        HttpMethod.POST.toString()),
                    new AntPathRequestMatcher(adminServer.path("/instances/*"),
                        HttpMethod.DELETE.toString()),
                    new AntPathRequestMatcher("/actuator/**")
                ))
            .rememberMe((rememberMe) -> rememberMe.key(UUID.randomUUID().toString()).tokenValiditySeconds(1209600));
    }
}
