/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.taskexecutor;

import ai.starwhale.mlops.domain.node.Device;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import javax.annotation.PostConstruct;

public class SourcePool {

    /**
     * ready to work
     */
    private static volatile boolean ready = false;

    private static final Queue<Device> idleDevices = new ArrayDeque<>();
    private static final Queue<Device> usingDevices = new ArrayDeque<>();

    @PostConstruct
    public static void init() {
        // todo: detect current machine devices

    }

    public static Queue<Device> getIdleDevices() {
        return idleDevices;
    }

    /**
     * whether init successfully
     */
    public static boolean isReady() {
        return ready;
    }

    // todo with function and middle state
    public static synchronized Set<Device> allocate(int num) {
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
        throw new RuntimeException();
    }
    // todo with function and middle state
    public static synchronized void free(Set<Device> devices) {
        if (ready) {
            for (Device device : devices) {
                usingDevices.remove(device);
                idleDevices.add(device);
            }
        }
        throw new RuntimeException();
    }

}
