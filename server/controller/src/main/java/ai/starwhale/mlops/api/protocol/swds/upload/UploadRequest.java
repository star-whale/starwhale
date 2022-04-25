/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.swds.upload;

import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class UploadRequest {

    @NotNull
    String swds;
    @NotNull
    UploadPhase phase;
    String force;
    String project;

    static final String FORCE = "1";

    public boolean force() {
        return FORCE.equals(force);
    }

    public String getProject() {
        return null == project ? "" : project;
    }

}
