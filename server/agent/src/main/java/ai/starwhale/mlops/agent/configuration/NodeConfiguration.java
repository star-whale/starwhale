/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.configuration;

import ai.starwhale.mlops.agent.node.SourcePool;
import ai.starwhale.mlops.agent.node.gpu.DeviceDetect;
import ai.starwhale.mlops.agent.node.gpu.NvidiaDetect;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NodeConfiguration {

    @Bean
    public SourcePool sourcePool(Map<String, DeviceDetect> gpuDetectImpls) {
        return new SourcePool(gpuDetectImpls);
    }

    @Bean
    public DeviceDetect nvidiaGPUDetect() {
        return new NvidiaDetect();
    }

    // todo:other brand of gpu

}
