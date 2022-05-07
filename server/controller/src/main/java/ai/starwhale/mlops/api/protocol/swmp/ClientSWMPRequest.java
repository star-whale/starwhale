/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
