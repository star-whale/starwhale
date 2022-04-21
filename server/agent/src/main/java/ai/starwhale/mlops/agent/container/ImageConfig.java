/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.agent.container;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ImageConfig {

    private String image;
    private List<String> env;
    private Map<String, String> labels;
    private Boolean autoRemove;
    private List<String> entrypoint;
    /**
     * Set the Network mode for the container
     * <ul>
     * <li>'bridge': creates a new network stack for the container on the docker bridge</li>
     * <li>'none': no networking for this container</li>
     * <li>'container:<name|id>': reuses another container network stack</li>
     * <li>'host': use the host network stack inside the container. Note: the host mode gives the container full access to local system
     * services such as D-bus and is therefore considered insecure.</li>
     * </ul>
     */
    private String networkMode;

    private List<String> cmd;

    private CPUConfig cpuConfig;
    private GPUConfig gpuConfig;
    private IOConfig ioConfig;

    @Singular private List<Mount> mounts;

    @Data
    @Builder
    public static class GPUConfig {

        private String driver;

        private Integer count;

        private List<String> deviceIds;

        private List<List<String>> capabilities;
    }

    @Data
    @Builder
    public static class CPUConfig {

        private Long cpuPeriod;

        private Long cpuQuota;

        private Long cpuCount;

        private Long cpuPercent;
    }

    @Data
    @Builder
    public static class IOConfig {

        private Long ioMaximumIOps;

        private Long ioMaximumBandwidth;
    }

    @Data
    @Builder
    public static class Mount {

        /**
         * <ul>
         *     <li>BIND</li>
         *     <li>VOLUME</li>
         *     <li>TMPFS</li>
         *     <li>NPIPE</li>
         * </ul>
         */
        private String type;

        private String source;

        private String target;

        private Boolean readOnly;


    }

}
