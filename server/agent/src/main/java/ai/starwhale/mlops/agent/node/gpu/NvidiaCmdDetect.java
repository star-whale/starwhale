/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.gpu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * detect GPU info by nvidia-smi cmd(dependency direct running on the host)
 */
@Slf4j
public class NvidiaCmdDetect implements GPUDetect {

    private final static String detectCmd = "nvidia-smi -q -x";

    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public Optional<List<GPUInfo>> detect() {
        try {
            String gpuXmlInfo = getGpuXmlInfo();
            if (StringUtils.hasText(gpuXmlInfo)) {
                List<GPUInfo> gpuInfos = convertXml(gpuXmlInfo);
                return Optional.of(gpuInfos);
            }
        } catch (Exception e) {
            log.error("detect gpu info occur error, message: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * get gpu info by cmd
     *
     * @return xml text
     * @throws IOException IO error
     */
    public String getGpuXmlInfo() throws IOException {
        java.lang.Process process;
        String result = "";
        process = Runtime.getRuntime().exec(detectCmd);
        try (InputStream inputStream = process.getInputStream()) {
            result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("exec cmd:{} occur io error, message: {}", detectCmd, e.getMessage(), e);
        } finally {
            if (process.isAlive()) {
                process.destroy();
            }
        }
        return result;
    }

    /**
     * Nvidia gpu info（by nvidia-smi）
     *
     * @return gpu usage info
     * @throws JsonProcessingException xml parse error
     */
    public List<GPUInfo> convertXml(String xmlText) throws JsonProcessingException {
        NvidiaSmiLog smiLog = xmlMapper.readValue(xmlText, NvidiaSmiLog.class);

        String driverVersion = smiLog.getDriverVersion();
        String cudaVersion = smiLog.getCudaVersion();
        List<GPUInfo> gpuInfoList = new ArrayList<>();
        smiLog.getGpu().forEach(gpu -> {

            String uuid = gpu.getUuid();
            String productName = gpu.getProductName();
            String productBrand = gpu.getProductBrand();

            List<Process> processes = gpu.getProcesses();
            List<GPUInfo.ProcessInfo> processInfo = new ArrayList<>();
            processes.forEach(process -> {
                String pid = process.getPid();
                String name = process.getProcessName();
                String usedMemory = process.getUsedMemory();
                processInfo.add(
                        GPUInfo.ProcessInfo.builder().pid(pid).name(name).usedMemory(usedMemory).build()
                );
            });

            FbMemoryUsage fbMemoryUsage = gpu.getFbMemoryUsage();
            String total = fbMemoryUsage.getTotal();
            String used = fbMemoryUsage.getUsed();
            String free = fbMemoryUsage.getFree();
            int intTotal = Integer.parseInt(total.split(" ")[0]);
            int intUsed = Integer.parseInt(used.split(" ")[0]);

            GPUInfo gpuInfo = GPUInfo.builder()
                    .id(uuid)
                    .name(productName)
                    .brand(productBrand)
                    .totalMemory(total)
                    .freeMemory(free)
                    .usedMemory(used)
                    .driverInfo(String.format("driver version:%s,cuda version:%s", driverVersion, cudaVersion))
                    .usageRate((int) ((float) intUsed / intTotal * 100))
                    .processInfos(processInfo)
                    .build();

            gpuInfoList.add(gpuInfo);
        });
        return gpuInfoList;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
class NvidiaSmiLog {
    @JsonProperty("driver_version")
    private String driverVersion;
    @JsonProperty("cuda_version")
    private String cudaVersion;
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<GPU> gpu;
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
class GPU {
    private String id;
    @JsonProperty("product_name")
    private String productName;
    @JsonProperty("product_brand")
    private String productBrand;
    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("fb_memory_usage")
    private FbMemoryUsage fbMemoryUsage;
    private List<Process> processes;
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
class FbMemoryUsage {
    private String total;
    private String used;
    private String free;

}

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
class Process {

    private String pid;
    @JsonProperty("process_name")
    private String processName;
    @JsonProperty("used_memory")
    private String usedMemory;

}
