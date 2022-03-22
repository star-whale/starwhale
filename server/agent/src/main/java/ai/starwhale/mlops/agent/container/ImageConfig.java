/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.agent.container;

public class ImageConfig {
    private String image;
    private String[] env;
    private Boolean autoRemove;
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

    private CPUConfig cpuConfig;
    private IOConfig ioConfig;

    private Mount mount;

    static class CPUConfig {
        private Long cpuPeriod;

        private Long cpuRealtimePeriod;

        private Long cpuRealtimeRuntime;

        private Integer cpuShares;

        /**
         * @since 1_20
         */
        private Long cpuQuota;

        private String cpusetCpus;

        private String cpusetMems;

        private Long cpuCount;

        private Long cpuPercent;
    }

    static class IOConfig {
        private Long ioMaximumIOps;

        private Long ioMaximumBandwidth;
    }

    static class Mount {
        private MountType type;

        private String source;

        private String target;

        private Boolean readOnly;

        enum MountType {
            BIND,
            VOLUME,
            TMPFS,
            NPIPE
        }

    }

}
