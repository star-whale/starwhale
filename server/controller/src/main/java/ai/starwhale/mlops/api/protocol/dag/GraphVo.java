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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GraphVo {

    @NotNull
    String id;

    /**
     * key: group name. e.g. PPL Task/ CMP Task/ Job
     */
    Map<String, List<GraphNodeVo>> groupingNodes = new HashMap<>();

    @NotNull
    List<GraphEdgeVo> edges;

    public GraphVo(Graph graph) {
        if (graph.empty()) {
            edges = new ArrayList<>(0);
            return;
        }
        // this.id = graph.getId().toString();
        graph.getGroupingNodes().entrySet().stream().forEach(entry -> {
            List<GraphNodeVo> nodeVos = entry.getValue().parallelStream()
                    .map(graphNode -> new GraphNodeVo(graphNode)).collect(
                            Collectors.toList());
            groupingNodes.put(entry.getKey(), nodeVos);
        });
        edges = graph.getEdges().parallelStream().map(graphEdge -> new GraphEdgeVo(graphEdge))
                .collect(Collectors.toList());
    }

    @Data
    public static class GraphNodeVo {

        /**
         * id of the node
         */
        @NotNull
        String id;

        /**
         * type of the node e.g. TaskStatus or JobStatus
         */
        @NotNull
        String type;

        /**
         * content for the node e.g. status name
         */
        Object content;

        public GraphNodeVo(GraphNode graphNode) {
            this.id = graphNode.getId().toString();
            this.type = graphNode.getType();
            this.content = graphNode.getContent();
        }
    }

    @Data
    public static class GraphEdgeVo {

        /**
         * id for node of edge start
         */
        @NotNull
        String from;

        /**
         * id for node of edge end
         */
        @NotNull
        String to;

        /**
         * info for the edge. e.g. timestamp
         */
        String content;

        public GraphEdgeVo(GraphEdge graphEdge) {
            this.from = graphEdge.getFrom().toString();
            this.to = graphEdge.getTo().toString();
            this.content = graphEdge.getContent();
        }
    }

}
