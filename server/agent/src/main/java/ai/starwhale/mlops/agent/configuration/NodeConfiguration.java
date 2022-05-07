/**
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

package ai.starwhale.mlops.agent.configuration;

import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.node.base.SimpleSystemDetect;
import ai.starwhale.mlops.agent.node.base.SystemDetect;
import ai.starwhale.mlops.agent.node.cpu.CPUDetect;
import ai.starwhale.mlops.agent.node.cpu.SimpleCPUDetect;
import ai.starwhale.mlops.agent.node.gpu.GPUDetect;
import ai.starwhale.mlops.agent.node.gpu.NvidiaCmdDetect;
import ai.starwhale.mlops.agent.node.initializer.SourcePoolInitializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class NodeConfiguration {

    @Bean
    @ConditionalOnProperty(name = "sw.agent.node.sourcePool.init.enabled", havingValue = "true", matchIfMissing = true)
    public SourcePoolInitializer sourcePoolInitializer() {
        return new SourcePoolInitializer();
    }

    @Bean
    public SourcePool sourcePool(Map<String, GPUDetect> gpuDetectImpl, CPUDetect cpuDetect) {
        return new SourcePool(gpuDetectImpl, cpuDetect);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.agent.node.sourcePool.gpu.nvidia.detect", havingValue = "cmd", matchIfMissing = true)
    public GPUDetect nvidiaGPUDetect() {
        return new NvidiaCmdDetect();
    }

    @Bean
    @ConditionalOnProperty(name = "sw.agent.node.sourcePool.cpu.detect", havingValue = "simple", matchIfMissing = true)
    public CPUDetect simpleCPUDetect() {
        return new SimpleCPUDetect();
    }

    @Bean
    @ConditionalOnProperty(name = "sw.agent.node.sourcePool.system.detect", havingValue = "simple", matchIfMissing = true)
    public SystemDetect simpleSystemDetect() {
        return new SimpleSystemDetect();
    }

    // todo:other brand of gpu

}
