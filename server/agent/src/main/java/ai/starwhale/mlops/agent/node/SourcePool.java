/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node;

import ai.starwhale.mlops.agent.exception.AllocationException;
import ai.starwhale.mlops.agent.node.gpu.DeviceDetect;
import ai.starwhale.mlops.agent.node.gpu.GPUInfo;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.node.Device.Status;
import cn.hutool.core.collection.CollectionUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
public class SourcePool {

    private final Object lock = new Object();

    private final Map<String, DeviceDetect> gpuDetectImpl;

    private final Set<Device> devices = new CopyOnWriteArraySet<>();

    public SourcePool(
        Map<String, DeviceDetect> gpuDetectImpl) {
        this.gpuDetectImpl = gpuDetectImpl;
    }

    public void refresh() {
        synchronized (lock) {
            devices.clear();
            devices.addAll(this.detectDevices());
        }
    }

    /**
     * whether init successfully
     */
    private volatile boolean ready = false;

    public boolean isReady() {
        return ready;
    }

    public void setToReady() {
        ready = true;
    }


    public Set<Device> allocate(AllocateRequest request) {
        synchronized (lock) {
            if (CollectionUtil.isNotEmpty(devices)) {
                // determine whether the conditions are met
                long idleGpuNum = devices.stream().filter(
                        device -> device.getStatus().equals(Status.idle) && device.getClazz()
                                .equals(Clazz.GPU)).count();
                if (idleGpuNum >= request.getGpuNum()) {
                    int tmp = request.getGpuNum();
                    Set<Device> results = new HashSet<>();

                    for (Device device : devices) {
                        if (device.getStatus().equals(Status.idle) && device.getClazz()
                                .equals(Clazz.GPU) && tmp > 0) {
                            device.setStatus(Status.busy);
                            results.add(device);
                            tmp--;
                        }
                    }
                    return results;
                }
            }
            throw new AllocationException("allocate device error");
        }

    }

    // todo with function and middle state
    public void release(Set<Device> devices) {
        refresh();
    }

    @Builder
    @Getter
    public static class AllocateRequest {

        int gpuNum;
        /* GeForce GTX 1070 Ti/GeForce RTX 2070/Tesla T4 */
        // Set<String> labels;
    }

    private Set<Device> detectDevices() {
        List<GPUInfo> gpuInfos = gpuDetectImpl.values().stream()
            // realtime detect
            .map(DeviceDetect::detect)
            // filter the empty result
            .filter(Optional::isPresent)
            // get the result
            .map(Optional::get)
            // flat from list to single object
            .flatMap(Collection::stream)
            // reduce these results
            .collect(Collectors.toList());
        // todo:cpu

        return gpuInfos.stream()
            .map(gpuInfo ->
                Device.builder()
                    .id(gpuInfo.getId())
                    .clazz(Clazz.GPU)
                    .driver(gpuInfo.getDriverInfo())
                    .type(gpuInfo.getName())
                    .status(CollectionUtil.isNotEmpty(gpuInfo.getProcessInfos()) ? Status.busy
                        : Status.idle)
                    .build())
            .collect(Collectors.toSet());
    }
}
