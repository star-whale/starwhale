/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
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
