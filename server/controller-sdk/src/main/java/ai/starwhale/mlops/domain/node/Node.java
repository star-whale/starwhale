/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.node;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Node is a machine/ a virtual machine or even a K8S pod in the cluster
 */
@Data
@Builder
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
     * todo by gxx: is it will be better if there coding with map which key is device and value is task?
     */
    List<DeviceHolder> deviceHolders;

}
