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

import ai.starwhale.mlops.domain.dag.po.GraphEdgeEntity;
import ai.starwhale.mlops.domain.dag.po.GraphEntity;
import ai.starwhale.mlops.domain.dag.po.GraphNodeEntity;
import ai.starwhale.mlops.domain.dag.po.GraphNodeEntity.NodeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    public Graph(GraphEntity graphEntity,List<GraphNodeEntity> nodeEntities,List<GraphEdgeEntity> edgeEntities){
        this.id = graphEntity.getId();
        nodeEntities.parallelStream().map(entity->new GraphNode(entity)).forEach(graphNode -> {
            safeGetNodesOfGroup(graphNode.getGroup()).add(graphNode);
        });
        edgeEntities.parallelStream().map(entity-> new GraphEdge(entity)).forEach(graphEdge -> {
            this.edges.add(graphEdge);
        });

    }

    private List<GraphNode> safeGetNodesOfGroup(String group) {
        return groupingNodes.computeIfAbsent(group,
            k -> Collections.synchronizedList(new LinkedList<>()));
    }

    public static Graph emptyInstance(){
        return new Graph();
    }

    public boolean empty(){
        return null == id;
    }

    public void add(GraphNodeEntity nodeEntity) {
        GraphNode graphNode = new GraphNode(nodeEntity);
        safeGetNodesOfGroup(graphNode.getGroup()).add(graphNode);
    }

    public GraphNode findLastNodeOf(String group,NodeType nodeType, Long ownerId) {

        GraphNode lastNode = List.copyOf(safeGetNodesOfGroup(group)).parallelStream().filter(graphNode -> graphNode.getNodeOwnerId().equals(ownerId)
            && graphNode.getType() == nodeType)
            .reduce(GraphNode.emptyInstance(), BinaryOperator.maxBy(Comparator.comparing(GraphNode::getId)) );
        return lastNode;
    }

    public void add(GraphEdgeEntity graphEdgeEntity) {
        edges.add(new GraphEdge(graphEdgeEntity));
    }
}
