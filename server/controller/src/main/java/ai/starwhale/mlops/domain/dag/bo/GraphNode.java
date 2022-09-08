/*
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

package ai.starwhale.mlops.domain.dag.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * node on the graph
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphNode {

    /**
     * id of the node
     */
    Long id;

    /**
     * type of the node e.g. Task, Step, Job
     */
    String type;

    /**
     * content for the node e.g. status name
     */
    Object content;

    String group;

    Long entityId;

    public static final Long ID_FAKE = -1L;

    public boolean empty() {
        return null == this.id || ID_FAKE.equals(this.id);
    }

    public static GraphNode emptyInstance() {
        return GraphNode.builder().id(ID_FAKE).build();
    }
}
