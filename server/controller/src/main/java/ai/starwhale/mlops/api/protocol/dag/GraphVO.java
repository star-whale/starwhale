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

package ai.starwhale.mlops.api.protocol.dag;


import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.dag.bo.GraphEdge;
import ai.starwhale.mlops.domain.dag.bo.GraphNode;
import ai.starwhale.mlops.domain.dag.po.GraphNodeEntity.NodeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class GraphVO {

    String id;

    /**
     * key: group name. e.g. PPL Task/ CMP Task/ Job
     */
    Map<String, List<GraphNodeVO>> groupingNodes = new HashMap<>();

    List<GraphEdgeVO> edges;

    public GraphVO(Graph graph){
        if(graph.empty()){
            edges = new ArrayList<>(0);
            return;
        }
        this.id = graph.getId().toString();
        graph.getGroupingNodes().entrySet().stream().forEach(entry->{
            List<GraphNodeVO> nodeVOS = entry.getValue().parallelStream()
                .map(graphNode -> new GraphNodeVO(graphNode)).collect(
                    Collectors.toList());
            groupingNodes.put(entry.getKey(),nodeVOS);
        });
        edges = graph.getEdges().parallelStream().map(graphEdge -> new GraphEdgeVO(graphEdge)).collect(Collectors.toList());
    }

    @Data
    public static class GraphNodeVO{
        /**
         * id of the node
         */
        String id;

        /**
         * type of the node e.g. TaskStatus or JobStatus
         */
        NodeType type;

        /**
         * content for the node e.g. status name
         */
        String content;

        public GraphNodeVO(GraphNode graphNode){
            this.id = graphNode.getId().toString();
            this.type = graphNode.getType();
            this.content = graphNode.getContent();
        }
    }

    @Data
    public static class GraphEdgeVO{
        /**
         * id for node of edge start
         */
        String from;

        /**
         * id for node of edge end
         */
        String to;

        /**
         * info for the edge. e.g. timestamp
         */
        String content;

        public GraphEdgeVO(GraphEdge graphEdge) {
            this.from = graphEdge.getFrom().toString();
            this.to = graphEdge.getTo().toString();
            this.content = graphEdge.getContent();
        }
    }

}
