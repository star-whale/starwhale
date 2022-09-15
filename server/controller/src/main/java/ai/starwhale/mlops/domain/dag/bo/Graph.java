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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DAG
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Graph {

    Long id;

    /**
     * key: group name. e.g. PPL Task/ CMP Task/ Job
     */
    Map<String, List<GraphNode>> groupingNodes = new ConcurrentHashMap<>();

    List<GraphEdge> edges = Collections.synchronizedList(new ArrayList<>());

    private List<GraphNode> safeGetNodesOfGroup(String group) {
        return groupingNodes.computeIfAbsent(group,
                k -> Collections.synchronizedList(new LinkedList<>()));
    }


    public boolean empty() {
        return false;
    }

    public void add(GraphNode graphNode) {
        safeGetNodesOfGroup(graphNode.getGroup()).add(graphNode);
    }

    public void add(GraphEdge graphEdge) {
        edges.add(graphEdge);
    }
}
