/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.swds;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.domain.user.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SWDatasetVersionEntity extends BaseEntity {

    private Long id;

    private Long datasetId;

    private Long ownerId;

    private UserEntity owner;

    private String versionName;

    private String versionTag;

    private String versionMeta;

    private String filesUploaded;

    private String storagePath;

    /**
     * 0 - unavailable
     * 1 - available
     */
    private Integer status = STATUS_UN_AVAILABLE;

    public static final Integer STATUS_AVAILABLE=1;
    public static final Integer STATUS_UN_AVAILABLE=0;
}
