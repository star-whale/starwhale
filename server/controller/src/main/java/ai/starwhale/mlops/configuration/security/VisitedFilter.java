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

import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.mapper.ProjectVisitedMapper;
import ai.starwhale.mlops.domain.user.UserService;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class VisitedFilter extends OncePerRequestFilter {

    @Resource
    private UserService userService;
    @Resource
    private ProjectService projectService;

    @Resource
    private ProjectVisitedMapper mapper;

    private final Map<Long, Visit> visitedProjectCacheMap = new ConcurrentHashMap<>();
    private static final long storageInterval = 5 * 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Set<String> projects = (Set<String>) request.getAttribute(ProjectDetectionFilter.ATTRIBUTE_PROJECT);
        String projectUrl = projects.stream().findAny().orElse("0");

        if (!Objects.equals("0", projectUrl)) {
            Long projectId = projectService.getProjectId(projectUrl);
            Long userId = userService.currentUserDetail().getId();

            Visit visit = new Visit(projectId, System.currentTimeMillis());
            Visit lastVisit = visitedProjectCacheMap.get(userId);

            if (lastVisit == null || needStorage(visit, lastVisit)) {
                visitedProjectCacheMap.put(userId, visit);
                mapper.insert(userId, projectId);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean needStorage(Visit current, Visit previous) {
        return !Objects.equals(current.projectId, previous.projectId)
                && current.time > previous.time + storageInterval;
    }

    @AllArgsConstructor
    static class Visit {

        Long projectId;
        long time;
    }
}
