/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.configuration;

import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.node.cpu.CPUDetect;
import ai.starwhale.mlops.agent.node.cpu.SimpleCPUDetect;
import ai.starwhale.mlops.agent.node.gpu.GPUDetect;
import ai.starwhale.mlops.agent.node.gpu.NvidiaCmdDetect;
import ai.starwhale.mlops.agent.node.host.HostDetect;
import ai.starwhale.mlops.agent.node.host.SimpleHostDetect;
import ai.starwhale.mlops.agent.node.initializer.SourcePoolInitializer;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class NodeConfiguration {

    @Bean
    public SourcePool sourcePool(Map<String, GPUDetect> gpuDetectImpl, CPUDetect cpuDetect) {
        return new SourcePool(gpuDetectImpl, cpuDetect);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.node.sourcePool.init.enabled", havingValue = "true", matchIfMissing = true)
    public SourcePoolInitializer sourcePoolInitializer(SourcePool sourcePool) {
        return new SourcePoolInitializer(sourcePool);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.node.sourcePool.gpu.nvidia.detect", havingValue = "cmd", matchIfMissing = true)
    public GPUDetect nvidiaGPUDetect(XmlMapper xmlMapper) {
        return new NvidiaCmdDetect(xmlMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "sw.node.sourcePool.cpu.detect", havingValue = "simple", matchIfMissing = true)
    public CPUDetect simpleCPUDetect() {
        return new SimpleCPUDetect();
    }
    @Bean
    @ConditionalOnProperty(name = "sw.node.sourcePool.host.detect", havingValue = "simple", matchIfMissing = true)
    public HostDetect simpleSystemDetect() {
        return new SimpleHostDetect();
    }

    // todo:other brand of gpu

}
