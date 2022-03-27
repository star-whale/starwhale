/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import ai.starwhale.mlops.agent.exception.AllocationException;
import ai.starwhale.mlops.domain.node.Device;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import javax.annotation.PostConstruct;

public class SourcePool {

    /**
     * ready to work
     */
    private volatile boolean ready = false;

    private final Queue<Device> idleDevices = new ArrayDeque<>();
    private final Queue<Device> usingDevices = new ArrayDeque<>();

    @PostConstruct
    public void init() {
        // todo: detect current machine devices

    }

    public Queue<Device> getIdleDevices() {
        return idleDevices;
    }

    /**
     * whether init successfully
     */
    public boolean isReady() {
        return ready;
    }

    public void setToReady() {
        ready = true;
    }

    // todo with function and middle state
    public synchronized Set<Device> allocate(int num) {
        if (ready) {
            if (idleDevices.size() >= num) {
                Set<Device> res = new HashSet<>();
                while (num > 0) {
                    Device idle = idleDevices.poll();
                    res.add(idle);
                    usingDevices.add(idle);
                    num--;
                }
                return res;
            }
        }
        throw new AllocationException("allocate device error");
    }
    // todo with function and middle state
    public synchronized void release(Set<Device> devices) {
        if (ready) {
            for (Device device : devices) {
                usingDevices.remove(device);
                idleDevices.add(device);
            }
        }
        throw new AllocationException("release device error");
    }

}
