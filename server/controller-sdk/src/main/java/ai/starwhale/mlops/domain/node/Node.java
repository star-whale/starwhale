/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import lombok.NoArgsConstructor;

/**
 * Node is a machine/ a virtual machine or even a K8S pod in the cluster
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    /**
     * the version of the agent that is deployed on this node
     */
    String agentVersion;

    /**
     * the unique number to identify this node
     */
    String serialNumber;

    /**
     * the ip address of this node
     */
    String ipAddr;

    /**
     * memory size in GB unit
     */
    Integer memorySizeGB;

    /**
     * the device holding information
     */
    List<Device> devices;

    public boolean equals(Object obj){
        if(!(obj instanceof Node)){
            return false;
        }
        Node node = (Node)obj;
        return this.serialNumber.equals(node.getSerialNumber());
    }

}
