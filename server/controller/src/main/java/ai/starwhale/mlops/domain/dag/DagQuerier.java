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

import ai.starwhale.mlops.api.protobuf.Job.JobVo.JobStatus;
import ai.starwhale.mlops.common.TimeConcern;
import ai.starwhale.mlops.domain.dag.bo.Graph;
import ai.starwhale.mlops.domain.dag.bo.GraphEdge;
import ai.starwhale.mlops.domain.dag.bo.GraphNode;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.step.StepHelper;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.job.step.status.StepStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.stereotype.Service;

@Service
public class DagQuerier {

    final JobDao jobDao;

    final StepHelper stepHelper;

    public DagQuerier(JobDao jobDao,
                      StepHelper stepHelper) {
        this.jobDao = jobDao;
        this.stepHelper = stepHelper;
    }

    public Graph dagOfJob(String jobUrl) {
        return buildGraph(jobDao.findJob(jobUrl));
    }

    private Graph buildGraph(Job job) {
        if (job.getStatus() == JobStatus.CREATED) {
            throw new SwValidationException(ValidSubject.JOB, "Job is still creating");
        }
        Graph graph = new Graph();
        AtomicLong idx = new AtomicLong(0);
        JobNodeContent initialJobNodeContent = new JobNodeContent(job);
        initialJobNodeContent.setStatus(JobStatus.CREATED);
        graph.add(jobNode(initialJobNodeContent, idx));
        Step stepPointer = stepHelper.firsStep(job.getSteps());
        long lastStepNodeId = idx.get();
        List<Long> lastTaskNodeIds = new LinkedList<>();
        do {
            graph.add(stepNode(new StepNodeContent(stepPointer), idx));
            if (lastTaskNodeIds.isEmpty()) {
                graph.add(new GraphEdge(lastStepNodeId, idx.get(), null));
            } else {
                for (Long taskNodeId : lastTaskNodeIds) {
                    graph.add(new GraphEdge(taskNodeId, idx.get(), null));
                }
            }
            lastTaskNodeIds.clear();
            lastStepNodeId = idx.get();
            List<Task> tasks = stepPointer.getTasks();
            for (int j = 0; j < tasks.size(); j++) {
                Task task = tasks.get(j);
                graph.add(taskNode(new TaskNodeContent(task), idx));
                graph.add(new GraphEdge(lastStepNodeId, idx.get(), null));
                lastTaskNodeIds.add(idx.get());
            }
            stepPointer = stepPointer.getNextStep();
        } while (stepPointer != null);
        graph.add(jobNode(new JobNodeContent(job), idx));
        for (Long taskNodeId : lastTaskNodeIds) {
            graph.add(new GraphEdge(taskNodeId, idx.get(), null));
        }
        return graph;
    }

    GraphNode taskNode(TaskNodeContent task, AtomicLong idx) {
        return GraphNode.builder().id(idx.incrementAndGet()).type(Task.class.getSimpleName()).content(
                task).entityId(task.getId()).group(Task.class.getSimpleName()).build();
    }

    GraphNode stepNode(StepNodeContent stepNodeContent, AtomicLong idx) {
        return GraphNode.builder().id(idx.incrementAndGet()).type(Step.class.getSimpleName()).content(
                stepNodeContent).entityId(stepNodeContent.getId()).group(Step.class.getSimpleName()).build();
    }

    GraphNode jobNode(JobNodeContent jobNodeContent, AtomicLong idx) {
        return GraphNode.builder().id(idx.incrementAndGet()).type(Job.class.getSimpleName()).content(
                jobNodeContent).entityId(jobNodeContent.getId()).group(Job.class.getSimpleName()).build();
    }

    @Data
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskNodeContent extends TimeConcern {

        Long id;
        String agentIp;
        TaskStatus status;

        public TaskNodeContent(Task t) {
            this.id = t.getId();
            this.agentIp = "Controller";
            this.status = t.getStatus();
            this.setStartTime(t.getStartTime());
            this.setFinishTime(t.getFinishTime());
        }


    }

    @Data
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobNodeContent extends TimeConcern {

        Long id;
        JobType jobType;
        JobStatus status;

        public JobNodeContent(Job job) {
            this.id = job.getId();
            this.jobType = job.getType();
            this.status = job.getStatus();
            this.setStartTime(job.getStartTime());
            this.setFinishTime(job.getFinishTime());
        }
    }

    @Data
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StepNodeContent extends TimeConcern {

        Long id;
        String name;
        StepStatus status;

        public StepNodeContent(Step step) {
            this.id = step.getId();
            this.name = step.getName();
            this.status = step.getStatus();
            this.setStartTime(step.getStartTime());
            this.setFinishTime(step.getFinishTime());
        }
    }


}
