/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.node.gpu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.util.StringUtils;

@Slf4j
public class NvidiaDetect implements DeviceDetect {

    private final static String detectCmd = "nvidia-smi -q -x";

    private static final String REG = "<!DOCTYPE.*.dtd\">";

    @Override
    public Optional<List<GPUInfo>> detect() {
        try {
            String gpuXmlInfo = getGpuXmlInfo();
            if (StringUtils.hasText(gpuXmlInfo)) {
                List<GPUInfo> gpuInfos = convertXml(gpuXmlInfo);
                return Optional.of(gpuInfos);
            }
        } catch (Exception e) {
            log.error("detect gpu info occur error , message : {}", e.getMessage(), e);
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
        Process process;
        String result = "";
        process = Runtime.getRuntime().exec(detectCmd);
        try (InputStream inputStream = process.getInputStream()) {
            result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("exec cmd:{}  occur io error, message : {}", detectCmd, e.getMessage(), e);
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
     * @throws DocumentException xml parse error
     */
    public List<GPUInfo> convertXml(String xmlText) throws DocumentException {
        // ignore dtd
        xmlText = xmlText.replaceAll(REG, "");
        Document document = DocumentHelper.parseText(xmlText);
        Element driverVersion = document.getRootElement().element("driver_version");
        Element cudaVersion = document.getRootElement().element("cuda_version");
        List<Element> gpu = document.getRootElement().elements("gpu");
        List<GPUInfo> gpuInfoList = new ArrayList<>();
        gpu.forEach(element -> {

            String uuid = element.element("uuid").getText();
            String productName = element.element("product_name").getText();
            String productBrand = element.element("product_brand").getText();
            Element fbMemoryUsage = element.element("fb_memory_usage");
            String total = fbMemoryUsage.element("total").getText();
            String used = fbMemoryUsage.element("used").getText();
            String free = fbMemoryUsage.element("free").getText();

            Element processes = element.element("processes");
            List<Element> infos = processes.elements("process_info");
            List<GPUInfo.ProcessInfo> processInfos = new ArrayList<>();
            infos.forEach(info -> {
                String pid = info.element("pid").getText();
                String name = info.element("process_name").getText();
                String usedMemory = info.element("used_memory").getText();
                processInfos.add(
                        GPUInfo.ProcessInfo.builder().pid(pid).name(name).usedMemory(usedMemory).build()
                );
            });
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
                .processInfos(processInfos)
                .build();

            gpuInfoList.add(gpuInfo);
        });
        return gpuInfoList;
    }
}
