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

package ai.starwhale.mlops.api.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.configuration.security.ProjectDetectionFilter;
import ai.starwhale.mlops.configuration.security.ProjectNameExtractor;
import java.io.IOException;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

public class ProjectDetectionFilterTest {

    @Test
    public void testDo() throws ServletException, IOException {
        ProjectNameExtractor nameExtractor = mock(ProjectNameExtractor.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(nameExtractor.extractProjectName(request)).thenReturn(Set.of("p"));
        ProjectDetectionFilter filter = new ProjectDetectionFilter(nameExtractor);
        filter.doFilter(request, mock(HttpServletResponse.class), mock(FilterChain.class));
        verify(request).setAttribute(ProjectDetectionFilter.ATTRIBUTE_PROJECT, Set.of("p"));
    }

}
