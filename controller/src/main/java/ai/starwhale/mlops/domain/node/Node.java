/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.node;

import lombok.Data;

import java.util.List;

/**
 * Node is a machine/ a virtual machine or even a K8S pod in the cluster
 */
@Data
public class Node {

    /**
     * unique name in the cluster
     */
    String name;

    /**
     * the ip address of this node
     */
    String ipAddr;

    /**
     * memory size in GB unit
     */
    Long memorySizeGB;

    /**
     * the device holding information
     */
    List<DeviceHolder> deviceHolders;

}
