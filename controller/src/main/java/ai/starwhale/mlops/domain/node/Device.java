/*
 * Copyright 2022.1-2022
 * StarWhale.com All right reserved. This software is the confidential and proprietary information of
 * StarWhale.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.domain.node;

import lombok.Data;

/**
 * Device is a computational unit such as GPU/ CPU or TPU which is the core resource for a Node to schedule
 */
@Data
public class Device {

    /**
     * gpu number or cpu number
     */
    String id;

    /**
     * device class
     */
    Clazz clazz;


    /**
     * P4 / 1070Ti etc.
     */
    String type;

    /**
     * CUDA 10.1 or something alike
     */
    String driver;

    /**
     * the device class CPU or GPU
     */
    public enum Clazz{
        CPU,GPU
    }

}
