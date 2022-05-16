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
import ai.starwhale.mlops.domain.dag.bo.GraphNode;
import ai.starwhale.mlops.domain.dag.mapper.GraphEdgeMapper;
import ai.starwhale.mlops.domain.dag.mapper.GraphMapper;
import ai.starwhale.mlops.domain.dag.mapper.GraphNodeMapper;
import ai.starwhale.mlops.domain.dag.po.GraphEdgeEntity;
import ai.starwhale.mlops.domain.dag.po.GraphEntity;
import ai.starwhale.mlops.domain.dag.po.GraphNodeEntity;
import ai.starwhale.mlops.domain.dag.po.GraphNodeEntity.NodeType;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.task.cache.LivingTaskCache;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Order(1)
public class DAGEditor implements CommandLineRunner {

    final GraphMapper graphMapper;
    final GraphNodeMapper graphNodeMapper;
    final GraphEdgeMapper graphEdgeMapper;
    final Map<Long, Graph> graphCache;
    final JobMapper jobMapper;
    final JobStatusMachine jobStatusMachine;
    final DAGQuerier dagQuerier;
    final LivingTaskCache taskCache;

    public DAGEditor(GraphNodeMapper graphNodeMapper,
        GraphMapper graphMapper,
        GraphEdgeMapper graphEdgeMapper,
        JobMapper jobMapper, JobStatusMachine jobStatusMachine,
        DAGQuerier dagQuerier,@Qualifier("cacheWrapperReadOnly") LivingTaskCache taskCache) {
        this.graphNodeMapper = graphNodeMapper;
        this.graphMapper = graphMapper;
        this.graphEdgeMapper = graphEdgeMapper;
        this.jobMapper = jobMapper;
        this.jobStatusMachine = jobStatusMachine;
        this.dagQuerier = dagQuerier;
        this.taskCache = taskCache;
        graphCache = new ConcurrentHashMap<>();
    }

    @Async
    public void taskStatusChange(Task task, TaskStatus newStatus){
        //find last node of task
        Long jobId = task.getJob().getId();
        Graph graph = graphCache.get(jobId);
        if(null == graph || graph.empty()){
            log.error("graph of job {} shall exists but no ",jobId);
            return;
        }
        NodeType nodeType = task.getTaskType() == TaskType.PPL?NodeType.TASK_PPL:NodeType.TASK_CMP;
        GraphNode lastNodeOfTask = graph.findLastNodeOf(nodeType.name(), nodeType,
            task.getId());

        if(lastNodeOfTask.empty()){
            Long newNodeId = addNodeToGraph(nodeType, taskNodeContent(task, newStatus), task.getId(), graph);
            if(task.getTaskType() == TaskType.CMP){
                taskCache.ofJob(jobId).parallelStream().filter(t->t.getTaskType() == TaskType.PPL)
                    .forEach(pplTask->{
                        GraphNode pplTaskLastNode = graph.findLastNodeOf(NodeType.TASK_PPL.name(), NodeType.TASK_PPL,
                            pplTask.getId());
                        addEdgeToGraph(pplTaskLastNode.getId(),newNodeId,graph);
                    });
            }else {
                GraphNode jobNode = graph.findLastNodeOf(NodeType.JOB.name(), NodeType.JOB,
                    jobId);
                addEdgeToGraph(jobNode.getId(),newNodeId,graph);
            }

        }else {
            if(lastNodeOfTask.getContent().contains(newStatus.name())){
                log.debug("task status the same no node will add to graph");
                return;
            }
            Long newNodeId = addNodeToGraph(nodeType,taskNodeContent(task, newStatus), task.getId(), graph);
            addEdgeToGraph(lastNodeOfTask.getId(),newNodeId,graph);
        }
        //if ppl empty, append to job ;if cmp empty, append to ppl tasks
        //if newStatus equals last node return
        //append status to task node
    }

    private String taskNodeContent(Task task, TaskStatus newStatus) {
        String host = null == task.getAgent() ? "controller" : task.getAgent().getIp();
        return host + ":" + newStatus.name();
    }

    @Async
    public void jobStatusChange(Job job, JobStatus newStatus){
        //find last node of job
        //if no, create one sync
        //if newStatus equals last node return
        //append status to job node sync
        //if newStatus is finished append node to cmp task
        //if newStatus is final remove from cache
        Long jobId = job.getId();
        Graph graph = graphCache.computeIfAbsent(jobId,k->Graph.emptyInstance());
        synchronized (graph){
            if(graph.empty()){
                if(jobStatusMachine.isFinal(newStatus)){
                    log.debug("final status and no graph in cache {}",newStatus);
                    return;
                }
                initGraphWithJob(newStatus, jobId, graph);
            }else {
                GraphNode graphNode = graph.findLastNodeOf(NodeType.JOB.name(),NodeType.JOB,jobId);
                if(newStatus.name().equals(graphNode.getContent())){
                    if(jobStatusMachine.isFinal(newStatus)){
                        graphCache.remove(jobId);
                    }
                    return;
                }
                Long newNodeId = addNodeToGraph(NodeType.JOB,newStatus.name(), jobId, graph);
                addEdgeToGraph(graphNode.getId(),newNodeId,graph);
                if(newStatus == JobStatus.SUCCESS){
                    Optional<Task> cmpTaskOpt = taskCache.ofJob(jobId).parallelStream()
                        .filter(task -> task.getTaskType() == TaskType.CMP).findFirst();
                    if(!cmpTaskOpt.isPresent()){
                        throw new SWValidationException(ValidSubject.JOB).tip("job success without cmp task");
                    }
                    Task cmpTask = cmpTaskOpt.get();
                    GraphNode cmpTaskNode = graph.findLastNodeOf(NodeType.TASK_CMP.name(),NodeType.TASK_CMP,cmpTask.getId());
                    addEdgeToGraph(cmpTaskNode.getId(),newNodeId,graph);
                }
                if(jobStatusMachine.isFinal(newStatus)){
                    log.info("remove graph cache for job {} because of final status ",jobId);
                    graphCache.remove(jobId);
                }
            }
        }


    }

    private void addEdgeToGraph(Long oldNodeId, Long newNodeId, Graph graph) {
        GraphEdgeEntity graphEdgeEntity = GraphEdgeEntity.builder().graphId(graph.getId()).from(oldNodeId)
            .to(newNodeId).content(String.valueOf(System.currentTimeMillis())).build();
        graphEdgeMapper.add(graphEdgeEntity);
        graph.add(graphEdgeEntity);
    }

    private void initGraphWithJob(JobStatus newStatus, Long jobId, Graph graph) {
        GraphEntity graphEntity = GraphEntity.builder().jobId(jobId).build();
        graphMapper.add(graphEntity);
        Long graphSqlId = graphEntity.getId();
        graph.setId(graphSqlId);
        addNodeToGraph(NodeType.JOB,newStatus.name(), jobId, graph);
    }

    private Long addNodeToGraph(NodeType nodeType,String content, Long ownerId, Graph graph) {
        GraphNodeEntity nodeEntity = GraphNodeEntity.builder()
            .content(content)
            .graphId(graph.getId())
            .nodeOwnerId(ownerId)
            .type(nodeType)
            .group(nodeType.name())
            .build();
        graphNodeMapper.add(nodeEntity);
        graph.add(nodeEntity);
        return nodeEntity.getId();
    }

    @Override
    public void run(String... args) throws Exception {
        List<JobStatus> hotJobStatuses = Arrays.asList(JobStatus.values())
            .parallelStream()
            .filter(jobStatus -> !jobStatusMachine.isFinal(jobStatus))
            .collect(Collectors.toList());
        List<JobEntity> runningJobs = jobMapper.findJobByStatusIn(hotJobStatuses);
        runningJobs.parallelStream().forEach(jobEntity -> {
            log.info("loading dag of {}",jobEntity.getId());
            graphCache.put(jobEntity.getId(),dagQuerier.dagOfJob(jobEntity.getId(),true));
        });
        log.info("dag loader loaded");

    }
}
