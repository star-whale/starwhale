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
import ai.starwhale.mlops.domain.job.JobManager;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.JobLoader;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.step.StepHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.task.TaskType;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
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

    final StepHelper stepHelper;

    final JobLoader jobLoader;

    public DAGQuerier(JobManager jobManager,
        HotJobHolder jobHolder, JobMapper jobMapper,
        StepHelper stepHelper, JobLoader jobLoader) {
        this.jobManager = jobManager;
        this.jobHolder = jobHolder;
        this.jobMapper = jobMapper;
        this.stepHelper = stepHelper;
        this.jobLoader = jobLoader;
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
        JobEntity jobEntity = jobMapper.findJobById(jobId);
        if(null == jobEntity){
            throw new SWValidationException(ValidSubject.JOB).tip("Job doesn't exists ");
        }
        List<Job> jobs = jobLoader.loadEntities(List.of(jobEntity), false, false);
        return buildGraphFromCache(jobs.get(0));
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
    }


}
