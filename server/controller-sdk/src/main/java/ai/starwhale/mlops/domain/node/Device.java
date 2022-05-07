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

package ai.starwhale.mlops.domain.node;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Device is a computational unit such as GPU/ CPU or TPU which is the core resource for a Node to schedule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
     * use status
     */
    Status status;

    /**
     * the device class CPU or GPU
     */
    public enum Clazz{
        CPU(1),GPU(2),UNKNOWN(-999);
        final int value;
        Clazz(int v){
            this.value = v;
        }
        public int getValue(){
            return this.value;
        }
        public static Clazz from(int v){
            for(Clazz deviceClass:Clazz.values()){
                if(deviceClass.value == v){
                    return deviceClass;
                }
            }
            return UNKNOWN;
        }

    }

    /**
     * status of device
     */
    public enum Status{
        idle,busy
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Device device = (Device) o;
        return id.equals(device.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
