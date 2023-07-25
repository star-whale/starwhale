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

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.common.util.HttpUtil;
import ai.starwhale.mlops.exception.SwNotFoundException;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ProjectDetectionFilter extends OncePerRequestFilter {

    public static final String ATTRIBUTE_PROJECT = "PROJECT";

    final ProjectNameExtractor projectNameExtractor;

    public ProjectDetectionFilter(ProjectNameExtractor projectNameExtractor) {
        this.projectNameExtractor = projectNameExtractor;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NotNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            request.setAttribute(ATTRIBUTE_PROJECT, projectNameExtractor.extractProjectName(request));
        } catch (SwNotFoundException e) {
            HttpUtil.error(response, HttpServletResponse.SC_NOT_FOUND, Code.validationException, e.getMessage());
            return;
        }
        filterChain.doFilter(request, response);
    }

}
