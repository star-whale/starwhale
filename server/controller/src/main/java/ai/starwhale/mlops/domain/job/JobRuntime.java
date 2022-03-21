/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.domain.node.Device;
import lombok.Builder;
import lombok.Data;

/**
 *
 */
@Data
@Builder
public class JobRuntime {

    /**
     * specify the job to run on whether CPU or GPU
     */
    Device.Clazz deviceClass;

    /**
     * how many devices does this job need to run on ie. how many tasks shall be split from the job
     */
    Integer deviceAmount;

    /**
     * what is the running container's image
     */
    String baseImage;
}
