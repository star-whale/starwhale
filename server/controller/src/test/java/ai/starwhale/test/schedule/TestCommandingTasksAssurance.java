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

package ai.starwhale.test.schedule;

import ai.starwhale.mlops.api.protocol.TaskStatusInterface;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.JobRuntime;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.system.agent.bo.Agent;
import ai.starwhale.mlops.domain.system.agent.bo.Agent.AgentUnModifiable;
import ai.starwhale.mlops.domain.system.agent.AgentStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.reporting.ReportedTask;
import ai.starwhale.mlops.schedule.CommandingTasksAssurance;
import ai.starwhale.test.ObjectMockHolder;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link ai.starwhale.mlops.schedule.CommandingTasksAssurance}
 */
public class TestCommandingTasksAssurance {


    @Test
    public void testOnNodeReporting(){
        final CommandingTasksAssurance commandingTasksAssurance = ObjectMockHolder.commandingTasksAssurance();
        Agent agent = new Agent(1L,"1","10.199.2.2","10.199.2.2",null,null, AgentStatus.ONLINE);
        List<TaskCommand> taskCommands = List.of(
            new TaskCommand(CommandType.TRIGGER,Task.builder().id(1L).uuid("uu1").build()),
            new TaskCommand(CommandType.TRIGGER,Task.builder().id(2L).uuid("uu2").build()));
        commandingTasksAssurance.onTaskCommanding(taskCommands,new AgentUnModifiable(agent));
        List<TaskCommand> unproperTasks = commandingTasksAssurance.onNodeReporting(
            new AgentUnModifiable(agent), List.of(ReportedTask.from(TaskReport.builder().id(3L).status(
                TaskStatusInterface.PREPARING).build())));
        Assertions.assertEquals(2,unproperTasks.size());
        unproperTasks = commandingTasksAssurance.onNodeReporting(
            new AgentUnModifiable(agent), List.of(
                ReportedTask.from(TaskReport.builder().id(2L).status(TaskStatusInterface.PREPARING).build()),
            ReportedTask.from(TaskReport.builder().id(4L).status(TaskStatusInterface.RUNNING).build())));
        Assertions.assertEquals(1,unproperTasks.size());
        unproperTasks = commandingTasksAssurance.onNodeReporting(
            new AgentUnModifiable(agent), List.of(ReportedTask.from(TaskReport.builder().id(1L).status(
                TaskStatusInterface.FAIL).build())));
        Assertions.assertEquals(0,unproperTasks.size());
    }

    @Test
    public void testAgentStatusChange(){
        final CommandingTasksAssurance commandingTasksAssurance = ObjectMockHolder.commandingTasksAssurance();;
        Agent agent = new Agent(1L,"1","10.199.2.2","10.199.2.2",null,null, AgentStatus.ONLINE);
        Job job = Job.builder().jobRuntime(JobRuntime.builder().deviceClass(Clazz.CPU).build()).build();
        Step step = Step.builder().job(job).build();
        List<TaskCommand> taskCommands = List.of(
            new TaskCommand(CommandType.TRIGGER,Task.builder().id(1L).step(step).uuid("uu1").build()),
            new TaskCommand(CommandType.TRIGGER,Task.builder().id(2L).step(step).uuid("uu2").build()));
        commandingTasksAssurance.onTaskCommanding(taskCommands,new AgentUnModifiable(agent));
        commandingTasksAssurance.onAgentStatusChange(agent,AgentStatus.OFFLINE);

        List<TaskCommand> unproperTasks = commandingTasksAssurance.onNodeReporting(
            new AgentUnModifiable(agent), List.of(ReportedTask.from(TaskReport.builder().id(3L).status(
                TaskStatusInterface.PREPARING).build())));
        Assertions.assertEquals(0,unproperTasks.size());
    }
}
