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

package ai.starwhale.mlops.domain.dag;

import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.dag.mapper.GraphEdgeMapper;
import ai.starwhale.mlops.domain.dag.mapper.GraphMapper;
import ai.starwhale.mlops.domain.dag.mapper.GraphNodeMapper;
import ai.starwhale.mlops.domain.dag.po.GraphEdgeEntity;
import ai.starwhale.mlops.domain.dag.po.GraphEntity;
import ai.starwhale.mlops.domain.dag.po.GraphNodeEntity;
import ai.starwhale.mlops.domain.job.JobManager;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DAGQuerier {

    final GraphMapper graphMapper;
    final GraphNodeMapper graphNodeMapper;
    final GraphEdgeMapper graphEdgeMapper;

    @Resource
    private JobManager jobManager;

    public DAGQuerier(GraphMapper graphMapper,
        GraphNodeMapper graphNodeMapper,
        GraphEdgeMapper graphEdgeMapper) {
        this.graphMapper = graphMapper;
        this.graphNodeMapper = graphNodeMapper;
        this.graphEdgeMapper = graphEdgeMapper;
    }
    public Graph dagOfJob(String jobUrl, Boolean withTask){
        return dagOfJob(jobManager.getJobId(jobUrl), withTask);
    }
    public Graph dagOfJob(Long jobId, Boolean withTask){
        GraphEntity graphEntity = graphMapper.findByJobId(jobId);
        if(null == graphEntity){
            return Graph.emptyInstance();
        }
        Long entityId = graphEntity.getId();
        List<GraphNodeEntity> graphNodeEntities = graphNodeMapper.findByGraphId(entityId);
        List<GraphEdgeEntity> graphEdgeEntities = graphEdgeMapper.findByGraphId(entityId);
        return new Graph(graphEntity,graphNodeEntities,graphEdgeEntities);
    }

    public Graph dagOfTask(Long taskId){
        List<GraphNodeEntity> graphNodeEntities = graphNodeMapper.findByOwnerId(taskId);
        Graph graph = Graph.emptyInstance();
        if(null == graphNodeEntities || graphNodeEntities.isEmpty()){
            return graph;
        }
        List<Long> nodeIds = graphNodeEntities.parallelStream().map(GraphNodeEntity::getId)
            .collect(Collectors.toList());
        List<GraphEdgeEntity> edgeEntities= graphEdgeMapper.scopeOf(nodeIds);
        graph.setId(graphNodeEntities.get(0).getGraphId());
        graphNodeEntities.forEach(graphNodeEntity -> graph.add(graphNodeEntity));
        edgeEntities.forEach(edgeEntity -> graph.add(edgeEntity));

        return graph;
    }

}
