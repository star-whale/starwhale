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

package ai.starwhale.mlops.domain.upgrade.rollup.web;

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RollingUpdateFilter extends OncePerRequestFilter implements RollingUpdateStatusListener {

    private volatile boolean readyToServe;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!readyToServe) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            String message = "server is upgrading, web api are not ready now";
            response.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
            log.info(message);
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void onNewInstanceStatus(ServerInstanceStatus status) throws InterruptedException {
        if (status == ServerInstanceStatus.UP) {
            readyToServe = false;
        } else if (status == ServerInstanceStatus.DOWN) {
            readyToServe = true;
        }
    }

    @Override
    public void onOldInstanceStatus(ServerInstanceStatus status) {
        if (status == ServerInstanceStatus.DOWN) {
            readyToServe = true;
        }
    }
}
