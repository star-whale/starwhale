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
import ai.starwhale.mlops.domain.dag.bo.GraphEdge;
import ai.starwhale.mlops.domain.dag.bo.GraphNode;
import ai.starwhale.mlops.domain.job.Job;
import ai.starwhale.mlops.domain.job.JobEntity;
import ai.starwhale.mlops.domain.job.JobManager;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.Step;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.StepEntity;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.task.StepHelper;
import ai.starwhale.mlops.domain.task.TaskEntity;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DAGQuerier {

    final JobManager jobManager;

    final HotJobHolder jobHolder;

    final JobMapper jobMapper;

    final StepMapper stepMapper;

    final TaskMapper taskMapper;

    final StepConverter stepConverter;

    final StepHelper stepHelper;

    public DAGQuerier(JobManager jobManager,
        HotJobHolder jobHolder, JobMapper jobMapper,
        StepMapper stepMapper, TaskMapper taskMapper,
        StepConverter stepConverter, StepHelper stepHelper) {
        this.jobManager = jobManager;
        this.jobHolder = jobHolder;
        this.jobMapper = jobMapper;
        this.stepMapper = stepMapper;
        this.taskMapper = taskMapper;
        this.stepConverter = stepConverter;
        this.stepHelper = stepHelper;
    }
    public Graph dagOfJob(String jobUrl, Boolean withTask){
        return dagOfJob(jobManager.getJobId(jobUrl), withTask);
    }
    public Graph dagOfJob(Long jobId, Boolean withTask){

        Collection<Job> jobs = jobHolder.ofIds(List.of(jobId));
        if(null == jobs || jobs.isEmpty()){
            return buildGraphFromDB(jobId);
        }
        Job job = jobs.stream().findAny().get();
        return buildGraphFromCache(job);
    }

    private Graph buildGraphFromCache(Job job) {
        Graph graph = new Graph();
        AtomicLong idx = new AtomicLong(0);
        graph.add(jobInit(job.getId(), idx));
        Step stepPointer = stepHelper.firsStep(job.getSteps());
        long lastStepNodeId = idx.get();
        List<Long> lastTaskNodeIds = new LinkedList<>();
        do{
            graph.add(stepNode(stepPointer.getId(), stepPointer.getStatus(),idx));
            if(lastTaskNodeIds.isEmpty()){
                graph.add(new GraphEdge(lastStepNodeId, idx.get(),null));
            }else {
                for(Long taskNodeId:lastTaskNodeIds){
                    graph.add(new GraphEdge(taskNodeId, idx.get(),null));
                }
            }
            lastTaskNodeIds.clear();
            lastStepNodeId = idx.get();
            List<Task> tasks = stepPointer.getTasks();
            for(int j=0;j<tasks.size();j++){
                Task task = tasks.get(j);
                graph.add(taskNode(task.getId(),task.getStatus(), idx));
                graph.add(new GraphEdge(lastStepNodeId, idx.get(),null));
                lastTaskNodeIds.add(idx.get());
            }
            stepPointer = stepPointer.getNextStep();
        }while (stepPointer != null);
        graph.add(jobNode(job.getId(),job.getStatus(), idx));
        for(Long taskNodeId:lastTaskNodeIds){
            graph.add(new GraphEdge(taskNodeId, idx.get(),null));
        }
        return graph;
    }

    private Graph buildGraphFromDB(Long jobId) {
        Graph graph = new Graph();
        AtomicLong idx = new AtomicLong(0);
        JobEntity jobEntity = jobMapper.findJobById(jobId);
        if(null == jobEntity){
            throw new SWValidationException(ValidSubject.JOB).tip("Job doesn't exists ");
        }
        graph.add(jobInit(jobId, idx));
        List<StepEntity> stepEntities = stepMapper.findByJobId(jobId);
        Map<Long, Long> linkMap = stepEntities.parallelStream()
            .collect(Collectors.toMap(StepEntity::getLastStepId, StepEntity::getId));
        stepEntities = stepEntities.stream().sorted((e1,e2)->{
            List<Long> e1NexIds = nextIds(linkMap,e1.getId());
            if(e1NexIds.contains(e2.getId())){
                return -1;
            }
            return 1;
        }).collect(Collectors.toList());
        long lastStepNodeId = idx.get();
        List<Long> lastTaskNodeIds = new LinkedList<>();
        for(int i=0;i<stepEntities.size();i++){
            StepEntity stepEntity=stepEntities.get(i);
            graph.add(stepNode(stepEntity.getId(),stepEntity.getStatus(), idx));
            if(lastTaskNodeIds.isEmpty()){
                graph.add(new GraphEdge(lastStepNodeId, idx.get(),null));
            }else {
                for(Long taskNodeId:lastTaskNodeIds){
                    graph.add(new GraphEdge(taskNodeId, idx.get(),null));
                }
            }
            lastTaskNodeIds.clear();
            lastStepNodeId = idx.get();
            List<TaskEntity> taskEntities = taskMapper.findByStepId(stepEntity.getId());
            for(int j=0;j<taskEntities.size();j++){
                TaskEntity taskEntity = taskEntities.get(j);
                graph.add(taskNode(taskEntity.getId(),taskEntity.getTaskStatus(), idx));
                graph.add(new GraphEdge(lastStepNodeId, idx.get(),null));
                lastTaskNodeIds.add(idx.get());
            }
        }
        graph.add(jobNode(jobEntity.getId(),jobEntity.getJobStatus(), idx));
        for(Long taskNodeId:lastTaskNodeIds){
            graph.add(new GraphEdge(taskNodeId, idx.get(),null));
        }
        return graph;
    }

    GraphNode taskNode(Long taskId, TaskStatus status, AtomicLong idx){
        return GraphNode.builder().id(idx.incrementAndGet()).type(Task.class.getSimpleName()).content(
            status.name()).entityId(taskId).group(Task.class.getSimpleName()).build();
    }

    GraphNode stepNode(Long stepId,StepStatus status, AtomicLong idx){
        return GraphNode.builder().id(idx.incrementAndGet()).type(Step.class.getSimpleName()).content(
            status.name()).entityId(stepId).group(Step.class.getSimpleName()).build();
    }

    GraphNode jobNode(Long jobId, JobStatus status, AtomicLong idx){
        return GraphNode.builder().id(idx.incrementAndGet()).type(Job.class.getSimpleName()).content(
            status.name()).entityId(jobId).group(Job.class.getSimpleName()).build();
    }

    GraphNode jobInit(Long jobId, AtomicLong idx){
        return GraphNode.builder().id(idx.incrementAndGet()).type(Job.class.getSimpleName()).content(
            JobStatus.CREATED.name()).entityId(jobId).group(Job.class.getSimpleName()).build();
    }

    private List<Long> nextIds(Map<Long, Long> linkMap,Long id){
        Long nextId = linkMap.get(id);
        List<Long> result = new LinkedList<>();
        if(null == nextId){
            return result;
        }
        result.add(nextId);
        result.addAll(nextIds(linkMap,nextId));
        return result;
    }


}
