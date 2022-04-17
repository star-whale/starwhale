/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.domain.swmp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Star Whale Model Package
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SWModelPackage {

    Long id;

    String name;

    String version;

    /**
     * The storage path of the swmp, it could be a directory or a single file
     */
    String path;

    public SWModelPackage copy(){
        return new SWModelPackage(this.id,this.name,this.version,this.path);
    }

}
