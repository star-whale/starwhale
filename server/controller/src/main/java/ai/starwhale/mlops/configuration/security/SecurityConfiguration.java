/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.configuration.security;

import ai.starwhale.mlops.domain.user.UserService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

import static java.lang.String.format;

@Slf4j
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true
)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Resource
    private UserService userService;

    @Resource
    private AccessDeniedHandler accessDeniedHandler;

    @Resource
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Resource
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Resource
    private AuthenticationFailureHandler authenticationFailureHandler;

    public SecurityConfiguration() {
        super();
        // Inherit security context ,so async function calls can effect
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService);
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().mvcMatchers("/static/**")
//            .antMatchers("/login**")// ignore the swagger index html/""'/
            .antMatchers("/swagger-ui/**")
            .antMatchers("/v3/api-docs/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Enable CORS and disable CSRF
        http = http.cors().and().csrf().disable();

        // Set session management to stateless
        http = http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and();

        // Set unauthorized requests exception handler
        http = http.exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint)
                .and();

        // Set permissions on endpoints
//        http.authorizeRequests()
//                // Login api must be publicly accessible
//                .antMatchers("/login").permitAll()
//                // Swagger endpoints must be publicly accessible
//                .antMatchers("/swagger-ui/**").permitAll()
//                .antMatchers("/v3/api-docs/**").permitAll()
//                // endpoints with role control can place here:use annotation
//
//                // all of other endpoints should be authenticated
//                .anyRequest().authenticated();


        // Add JWT token filter
        JwtLoginFilter jwtLoginFilter = new JwtLoginFilter();
        jwtLoginFilter.setAuthenticationManager(authenticationManagerBean());
        jwtLoginFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        jwtLoginFilter.setAuthenticationFailureHandler(authenticationFailureHandler);

        JwtTokenFilter jwtTokenFilter = new JwtTokenFilter();

        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(userService);

        http = http.authenticationProvider(jwtAuthenticationProvider)
            .authorizeRequests()
//            .antMatchers("/login").permitAll()
//                // Swagger endpoints must be publicly accessible
//            .antMatchers("/swagger-ui/**").permitAll()
//            .antMatchers("/v3/api-docs/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .and()
            .addFilterAt(jwtLoginFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtTokenFilter, JwtLoginFilter.class);
//        http = http.addFilterAt(jwtLoginFilter, UsernamePasswordAuthenticationFilter.class)
//            .addFilterAfter(jwtTokenFilter, JwtLoginFilter.class);

        //http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
    }

    // Expose authentication manager bean
    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

}
