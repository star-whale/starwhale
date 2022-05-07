/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swmp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SWMPObject {

    private String id;

    private String name;

    private String projectId;

    private String ownerId;

    private Version version;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Version {

        private String id;

        private String name;

        private String ownerId;

        private String tag;

        private String meta;

        private String storagePath;

    }
}
