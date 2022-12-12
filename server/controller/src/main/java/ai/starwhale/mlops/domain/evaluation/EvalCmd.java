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

package ai.starwhale.mlops.domain.evaluation;

import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.step.bo.Step;
import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.schedule.CmdGenerator;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EvalCmd implements CmdGenerator {

    /**
     * {instance}/project/{projectName}/dataset/{datasetName}/version/{version}
     */
    static final String FORMATTER_URI_DATASET = "%s/project/%s/dataset/%s/version/%s";

    @Value("${sw.instance-uri}")
    String instanceUri;

    private static final String SPACE = " ";

    public String genCmd(Task task) {
        Step step = task.getStep();
        Job job = step.getJob();
        List<String> cmds = new LinkedList<>();
        cmds.add("swcli -vvv eval run");
        cmds.add(String.format("--runtime %s/version/%s", job.getJobRuntime().getName(),
                job.getJobRuntime().getVersion()));
        cmds.add(String.format("--model %s/version/%s", job.getModel().getName(), job.getModel().getVersion()));
        for (var ds : job.getDataSets()) {
            cmds.add(String.format("--dataset " + FORMATTER_URI_DATASET, instanceUri, job.getProject().getName(),
                    ds.getName(), ds.getVersion()));
        }
        cmds.add(String.format("--version %s", job.getUuid()));
        cmds.add(String.format("--step=%s", step.getName()));
        cmds.add(String.format("--task-index=%d", task.getTaskRequest().getIndex()));
        cmds.add(String.format("--override-task-num=%d", task.getTaskRequest().getTotal()));

        return String.join(SPACE, cmds);
    }

    @Override
    public boolean apply(JobType type) {
        return JobType.EVALUATION == type;
    }

}
