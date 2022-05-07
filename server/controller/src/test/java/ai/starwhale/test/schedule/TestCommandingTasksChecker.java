/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.test.schedule;

import ai.starwhale.mlops.api.protocol.TaskStatusInterface;
import ai.starwhale.mlops.api.protocol.report.req.TaskReport;
import ai.starwhale.mlops.domain.system.agent.Agent;
import ai.starwhale.mlops.domain.system.agent.Agent.AgentUnModifiable;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.domain.task.bo.TaskCommand;
import ai.starwhale.mlops.domain.task.bo.TaskCommand.CommandType;
import ai.starwhale.mlops.domain.task.status.TaskStatusMachine;
import ai.starwhale.mlops.reporting.ReportedTask;
import ai.starwhale.mlops.schedule.CommandingTasksChecker;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestCommandingTasksChecker {
    final TaskStatusMachine taskStatusMachine = new TaskStatusMachine();
    final CommandingTasksChecker commandingTasksChecker = new CommandingTasksChecker(taskStatusMachine);

    @Test
    public void test(){
        Agent agent = new Agent(1L,"1","10.199.2.2","10.199.2.2",null,null);
        List<TaskCommand> taskCommands = List.of(
            new TaskCommand(CommandType.TRIGGER,Task.builder().id(1L).uuid("uu1").build()),
            new TaskCommand(CommandType.TRIGGER,Task.builder().id(2L).uuid("uu2").build()));
        commandingTasksChecker.onTaskCommanding(taskCommands,new AgentUnModifiable(agent));
        List<TaskCommand> unproperTasks = commandingTasksChecker.onNodeReporting(
            new AgentUnModifiable(agent), List.of(ReportedTask.from(TaskReport.builder().id(3L).status(
                TaskStatusInterface.PREPARING).build())));
        Assertions.assertEquals(2,unproperTasks.size());
        unproperTasks = commandingTasksChecker.onNodeReporting(
            new AgentUnModifiable(agent), List.of(
                ReportedTask.from(TaskReport.builder().id(2L).status(TaskStatusInterface.PREPARING).build()),
            ReportedTask.from(TaskReport.builder().id(4L).status(TaskStatusInterface.RUNNING).build())));
        Assertions.assertEquals(1,unproperTasks.size());
        unproperTasks = commandingTasksChecker.onNodeReporting(
            new AgentUnModifiable(agent), List.of(ReportedTask.from(TaskReport.builder().id(1L).status(
                TaskStatusInterface.FAIL).build())));
        Assertions.assertEquals(0,unproperTasks.size());
    }
}
