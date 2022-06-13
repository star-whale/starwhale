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

import ai.starwhale.mlops.common.TimeConcern;
import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.dag.bo.GraphEdge;
import ai.starwhale.mlops.domain.dag.bo.GraphNode;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.JobManager;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.StepConverter;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.job.step.StepHelper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private Graph dagOfJob(Long jobId, Boolean withTask){

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
        JobNodeContent initialJobNodeContent = new JobNodeContent(job);
        initialJobNodeContent.setStatus(JobStatus.CREATED);
        graph.add(jobNode(initialJobNodeContent, idx));
        Step stepPointer = stepHelper.firsStep(job.getSteps());
        long lastStepNodeId = idx.get();
        List<Long> lastTaskNodeIds = new LinkedList<>();
        do{
            graph.add(stepNode(new StepNodeContent(stepPointer),idx));
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
                graph.add(taskNode(new TaskNodeContent(task), idx));
                graph.add(new GraphEdge(lastStepNodeId, idx.get(),null));
                lastTaskNodeIds.add(idx.get());
            }
            stepPointer = stepPointer.getNextStep();
        }while (stepPointer != null);
        graph.add(jobNode(new JobNodeContent(job), idx));
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
        JobNodeContent initialJobNodeContent = new JobNodeContent(jobEntity);
        initialJobNodeContent.setStatus(JobStatus.CREATED);
        graph.add(jobNode(initialJobNodeContent, idx));
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
            graph.add(stepNode(new StepNodeContent(stepEntity), idx));
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
                graph.add(taskNode(new TaskNodeContent(taskEntity), idx));
                graph.add(new GraphEdge(lastStepNodeId, idx.get(),null));
                lastTaskNodeIds.add(idx.get());
            }
        }
        graph.add(jobNode(new JobNodeContent(jobEntity), idx));
        for(Long taskNodeId:lastTaskNodeIds){
            graph.add(new GraphEdge(taskNodeId, idx.get(),null));
        }
        return graph;
    }

    GraphNode taskNode(TaskNodeContent task, AtomicLong idx){
        return GraphNode.builder().id(idx.incrementAndGet()).type(Task.class.getSimpleName()).content(
            task).entityId(task.getId()).group(Task.class.getSimpleName()).build();
    }

    GraphNode stepNode(StepNodeContent stepNodeContent, AtomicLong idx){
        return GraphNode.builder().id(idx.incrementAndGet()).type(Step.class.getSimpleName()).content(
            stepNodeContent).entityId(stepNodeContent.getId()).group(Step.class.getSimpleName()).build();
    }

    GraphNode jobNode(JobNodeContent jobNodeContent, AtomicLong idx){
        return GraphNode.builder().id(idx.incrementAndGet()).type(Job.class.getSimpleName()).content(
            jobNodeContent).entityId(jobNodeContent.getId()).group(Job.class.getSimpleName()).build();
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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskNodeContent extends TimeConcern {
        Long id;
        TaskType type;
        String agentIp;
        TaskStatus status;
        public TaskNodeContent(Task t){
            this.id = t.getId();
            this.type = t.getTaskType();
            this.agentIp = t.getAgent() == null ? "Controller":t.getAgent().getIp();
            this.status = t.getStatus();
            this.setStartTime(t.getStartTime());
            this.setFinishTime(t.getFinishTime());
        }
        public TaskNodeContent(TaskEntity t){
            this.id = t.getId();
            this.type = t.getTaskType();
            this.agentIp = t.getAgent() == null ? "Controller":t.getAgent().getAgentIp();
            this.status = t.getTaskStatus();
            this.setStartTime(toEpochMilli(t.getStartedTime()));
            this.setFinishTime(toEpochMilli(t.getFinishedTime()));
        }


    }
    static long toEpochMilli(LocalDateTime t) {
        if(null == t){
            return -1;
        }
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobNodeContent extends TimeConcern{
        Long id;
        JobType jobType;
        JobStatus status;
        public JobNodeContent(Job job){
            this.id = job.getId();
            this.jobType = job.getType();
            this.status = job.getStatus();
            this.setStartTime(job.getStartTime());
            this.setFinishTime(job.getFinishTime());
        }
        public JobNodeContent(JobEntity job){
            this.id = job.getId();
            this.jobType = job.getType();
            this.status = job.getJobStatus();
            this.setStartTime(toEpochMilli(job.getCreatedTime()));
            this.setFinishTime(toEpochMilli(job.getFinishedTime()));
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StepNodeContent extends TimeConcern{
        Long id;
        String name;
        StepStatus status;
        public StepNodeContent(Step step){
            this.id = step.getId();
            this.name = step.getName();
            this.status = step.getStatus();
            this.setStartTime(step.getStartTime());
            this.setFinishTime(step.getFinishTime());
        }
        public StepNodeContent(StepEntity step){
            this.id = step.getId();
            this.name = step.getName();
            this.status = step.getStatus();
            this.setStartTime(toEpochMilli(step.getStartedTime()));
            this.setFinishTime(toEpochMilli(step.getFinishedTime()));
        }
    }


}
