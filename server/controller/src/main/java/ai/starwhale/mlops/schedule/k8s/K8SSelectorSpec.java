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

package ai.starwhale.mlops.schedule.k8s;

import ai.starwhale.mlops.domain.node.Device.Clazz;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import java.util.Map;
import lombok.Getter;

/**
 * map controller device to k8s label selector & resource limit
 */
@Getter
public class K8SSelectorSpec {

    Map<String,String> labelSelector;
    V1ResourceRequirements resourceSelector;

    static final String RESOURCE_CPU = "cpu";

    static final Map<Clazz,String> deviceNameMap = Map.of(Clazz.CPU,RESOURCE_CPU,Clazz.GPU,"nvidia.com/gpu");

    /**
     *
     * @param deviceClass
     * @param amount example 1T, 2G, 2, 3.8m
     */
    public K8SSelectorSpec(Clazz deviceClass, String amount){
        String resourceName = deviceNameMap.getOrDefault(deviceClass, RESOURCE_CPU);
        this.resourceSelector = new V1ResourceRequirements().limits(Map.of(resourceName,new Quantity(amount)));
        this.labelSelector = null; //node selector not used currently
    }

}
