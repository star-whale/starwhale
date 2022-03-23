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
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import javax.annotation.PostConstruct;

public class SourcePool {

    /**
     * ready to work
     */
    private static volatile boolean canRun = false;

    private static final Queue<Device> idleDevices = new ArrayDeque<>();
    private static final Set<Device> usingDevices = new HashSet<>();

    @PostConstruct
    public static void init() {
        // todo: detect current machine devices

    }

    public static synchronized Set<Device> apply(int num) {
        if (canRun) {
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

    public static synchronized void free(Set<Device> devices) {
        if (canRun) {
            for (Device device : devices) {
                usingDevices.remove(device);
                idleDevices.add(device);
            }
        }
        throw new RuntimeException();
    }

}
