/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.swmp;

import lombok.Data;

/**
 * request protocol of client
 */
@Data
public class ClientSWMPRequest {

    static final String SEPERATOR=":";

    /**
     * LATEST is not allowed in upload phase
     */
    static final String VERSION_LATEST="LATEST";

    /**
     * in formation of name:version
     */
    String swmp;

    String project;

    String force;

    public String name(){
        return swmp.split(SEPERATOR)[0];
    }

    public String version(){
        return swmp.split(SEPERATOR)[1];
    }

    static final String FORCE="1";
    public boolean force(){
        return FORCE.equals(force);
    }

    public String getProject() {
        return null == project ? "":project;
    }
}
