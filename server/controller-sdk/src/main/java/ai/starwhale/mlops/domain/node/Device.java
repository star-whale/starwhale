/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
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
