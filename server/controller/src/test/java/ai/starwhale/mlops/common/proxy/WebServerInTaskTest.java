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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.configuration.FeaturesProperties;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.task.bo.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebServerInTaskTest {
    private WebServerInTask webServerInTask;
    private HotJobHolder hotJobHolder;
    private FeaturesProperties featuresProperties;

    @BeforeEach
    void setUp() {
        hotJobHolder = mock(HotJobHolder.class);
        featuresProperties = mock(FeaturesProperties.class);
        webServerInTask = new WebServerInTask(hotJobHolder, featuresProperties);
    }

    @Test
    void getPrefix() {
        var prefix = webServerInTask.getPrefix();
        assertEquals("task", prefix);
    }

    @Test
    void getTarget() {
        // URI too short
        var thrown = assertThrows(IllegalArgumentException.class, () -> webServerInTask.getTarget("1"));
        assertEquals("invalid task URI: 1", thrown.getMessage());

        // no task found
        var notFound = "1/8000/";
        thrown = assertThrows(IllegalArgumentException.class, () -> webServerInTask.getTarget(notFound));
        assertEquals("can not find task 1", thrown.getMessage());

        // success
        var successUri = "1/8765/foo";
        var task = Task.builder().ip("1.2.3.4").id(1L).build();
        when(hotJobHolder.taskWithId(1L)).thenReturn(task);
        var target = webServerInTask.getTarget(successUri);
        assertEquals("http://1.2.3.4:8765/foo", target);
    }

    @Test
    void testGenerateGatewayUrl() {
        when(featuresProperties.isJobProxyEnabled()).thenReturn(true);
        var gatewayUrl = webServerInTask.generateGatewayUrl(1L, "1.1.1.1", 2);
        assertEquals("/gateway/task/1/2/", gatewayUrl);

        when(featuresProperties.isJobProxyEnabled()).thenReturn(false);
        gatewayUrl = webServerInTask.generateGatewayUrl(1L, "1.1.1.1", 2);
        assertEquals("http://1.1.1.1:2", gatewayUrl);
    }
}
