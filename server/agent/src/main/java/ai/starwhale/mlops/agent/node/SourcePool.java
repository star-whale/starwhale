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
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

@Slf4j
public class SourcePool {

    /**
     * ready to work
     */
    private AtomicBoolean ready = new AtomicBoolean(false);

    private final Map<String, DeviceDetect> gpuDetectImpls;

    private final Set<Device> devices = new CopyOnWriteArraySet<>();

    public SourcePool(
        Map<String, DeviceDetect> gpuDetectImpls) {
        this.gpuDetectImpls = gpuDetectImpls;
    }

    @PostConstruct
    public void init() {
        // todo: detect current machine devices

    }

    public void refresh() {
        devices.clear();
        devices.addAll(this.detectDevices());
    }

    /**
     * whether init successfully
     */
    public boolean isReady() {
        return ready.get();
    }

    public void setToReady() {
        ready.compareAndSet(false, true);
    }


    public synchronized Set<Device> allocate(AllocateRequest request) {
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

    // todo with function and middle state
    public void release(Set<Device> devices) {
        // todo
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
        List<GPUInfo> gpuInfos = gpuDetectImpls.values().stream()
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


    static AtomicBoolean a = new AtomicBoolean(false);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        Runnable r1 = () -> {
            log.info("i am r1");
            while (!test("r1", 10000)) {

            }
            latch.countDown();
        };
        Runnable r2 = () -> {
            log.info("i am r2");

            while (!test("r2", 6000)) {

            }

            latch.countDown();
        };
        Runnable r3 = () -> {
            log.info("i am r3");
            while (!test("r3", 2000)) {

            }

            latch.countDown();
        };

        Thread t1 = new Thread(r1);
        Thread t2 = new Thread(r2);
        Thread t3 = new Thread(r3);
        t1.start();
        t2.start();
        t3.start();

        latch.await(); // Wait for countdown
    }

    public static boolean test(String name, int sleep) {
        if (a.compareAndSet(false, true)) {
            log.info("{} come in!", name);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("i am {},exit", name);
            a.compareAndSet(true, false);
            return true;
        }
        return false;
    }
}
