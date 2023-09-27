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

package ai.starwhale.mlops.common.proxy;

import ai.starwhale.mlops.configuration.FeaturesProperties;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import org.springframework.stereotype.Component;

/**
 * proxy the request to the task
 * the uri is like "task/{taskId}/{port}/xxx"
 * we will get the ip of the task from task id and the port from the uri
 * and combine them to a target url
 * there may be multiple ports for a task, so we need to specify the port in the uri
 */
@Component
public class WebServerInTask implements Service {
    private final HotJobHolder hotJobHolder;
    private final FeaturesProperties featuresProperties;
    private static final String TASK_PREFIX = "task";
    private static final String TASK_GATEWAY_PATTERN = "/gateway/task/%d/%d/";

    public WebServerInTask(HotJobHolder hotJobHolder, FeaturesProperties featuresProperties) {
        this.hotJobHolder = hotJobHolder;
        this.featuresProperties = featuresProperties;
    }

    @Override
    public String getPrefix() {
        return TASK_PREFIX;
    }

    @Override
    public String getTarget(String uri) {
        // the uri is like "/{taskId}/{port}/xxx"
        var parts = uri.split("/", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException("invalid task URI: " + uri);
        }
        var taskId = Long.parseLong(parts[0]);
        var port = Long.parseLong(parts[1]);
        var path = parts[2];

        // get task from cache
        var task = hotJobHolder.taskWithId(taskId);
        if (null == task) {
            throw new IllegalArgumentException("can not find task " + taskId);
        }
        var ip = task.getIp();
        if (null == ip) {
            return null;
        }

        return "http://" + ip + ":" + port + "/" + path;
    }

    public String generateGatewayUrl(Long taskId, String taskIp, int port) {
        if (featuresProperties.isJobProxyEnabled()) {
            return String.format(TASK_GATEWAY_PATTERN, taskId, port);
        } else {
            return String.format("http://%s:%d", taskIp, port);
        }
    }
}
