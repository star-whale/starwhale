/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node;

import ai.starwhale.mlops.agent.exception.ErrorCode;
import ai.starwhale.mlops.agent.node.cpu.CPUDetect;
import ai.starwhale.mlops.agent.node.cpu.CPUInfo;
import ai.starwhale.mlops.agent.node.gpu.GPUDetect;
import ai.starwhale.mlops.agent.node.gpu.GPUInfo;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.node.Device.Status;
import cn.hutool.core.collection.CollectionUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
public class SourcePool {

    private final Object lock = new Object();

    private final Map<String, GPUDetect> gpuDetect;
    private final CPUDetect cpuDetect;

    private final Set<Device> devices = new CopyOnWriteArraySet<>();

    public SourcePool(
            Map<String, GPUDetect> gpuDetect, CPUDetect cpuDetect) {
        this.gpuDetect = gpuDetect;
        this.cpuDetect = cpuDetect;
    }

    public void refresh() {
        synchronized (lock) {
            devices.clear();
            devices.addAll(this.detectDevices());
        }
    }

    public Set<Device> getDevices() {
        return Set.copyOf(devices);
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


    public Set<Device> allocate(AllocateRequest request) throws Exception {
        Set<Device> allocates = internalAllocate(request);
        if (CollectionUtil.isEmpty(allocates))
            // no available device will throw Exception
            throw ErrorCode.allocateError.asException("allocate device fail, all busy");
        return allocates;
    }

    public Set<Device> preAllocateWithoutThrow(AllocateRequest request) {
        return internalAllocate(request);
    }

    private Set<Device> internalAllocate(AllocateRequest request) {
        synchronized (lock) {
            if (CollectionUtil.isNotEmpty(devices)) {
                // determine whether the conditions are met
                long idleGpuNum = devices.stream().filter(
                        device -> device.getStatus().equals(Status.idle) && device.getClazz()
                                .equals(Clazz.GPU)).count();
                long idleCpuNum = devices.stream().filter(
                        device -> device.getStatus().equals(Status.idle) && device.getClazz()
                                .equals(Clazz.CPU)).count();
                if (idleGpuNum >= request.getGpuNum() && idleCpuNum >= request.getCpuNum()) {
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

                    tmp = request.getCpuNum();
                    for (Device device : devices) {
                        if (device.getStatus().equals(Status.idle) && device.getClazz()
                                .equals(Clazz.CPU) && tmp > 0) {
                            device.setStatus(Status.busy);
                            results.add(device);
                            tmp--;
                        }
                    }
                    return results;
                }
            }
            return null;
        }
    }

    public void release(Set<Device> releaseDevices) {
        synchronized (lock) {
            if (CollectionUtil.isNotEmpty(devices)) {
                for (Device device : releaseDevices) {
                    Optional<Device> find = devices.stream().filter(d -> d.getId().equals(device.getId())).findFirst();
                    find.ifPresent(value -> value.setStatus(Status.idle));
                }
            }
        }
    }

    @Builder
    @Getter
    public static class AllocateRequest {

        int gpuNum;
        /* GeForce GTX 1070 Ti/GeForce RTX 2070/Tesla T4 */
        // Set<String> labels;
        int cpuNum;
    }

    private Set<Device> detectDevices() {
        List<GPUInfo> gpuInfos = gpuDetect.values().stream()
                // realtime detect
                .map(GPUDetect::detect)
                // filter the empty result
                .filter(Optional::isPresent)
                // get the result
                .map(Optional::get)
                // flat from list to single object
                .flatMap(Collection::stream)
                // reduce these results
                .collect(Collectors.toList());
        Set<Device> deviceSet = gpuInfos.stream()
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
        Optional<CPUInfo> cpuInfo = cpuDetect.detect();
        if (cpuInfo.isPresent()) {
            CPUInfo cpu = cpuInfo.get();
            for (int i = cpu.getCpuNum() - 1; i >= 0; i--) {
                deviceSet.add(
                        Device.builder()
                                .id(String.valueOf(i))
                                .status(Status.idle) // todo how to check one is idle, is "--cpu-period and --cpu-quota" more fit?
                                .clazz(Clazz.CPU)
                                .type(cpu.getCpuModel())
                                .build()
                );
            }
        }
        return deviceSet;
    }
}
