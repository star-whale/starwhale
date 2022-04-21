package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TaskJobStatusHelper {

    public JobStatus desiredJobStatus(Collection<Task> tasks){
        Map<TaskType, List<Task>> taskTypeListMap = tasks.parallelStream()
            .collect(Collectors.groupingBy(Task::getTaskType));
        List<Task> determiningTasks;
        List<Task> cmpTasks = taskTypeListMap.get(TaskType.CMP);
        if(null != cmpTasks && !cmpTasks.isEmpty()){
            determiningTasks = cmpTasks;
        }else {
            determiningTasks = taskTypeListMap.get(TaskType.PPL);
        }
        final JobStatus desiredJobStatuses = determiningTasks.parallelStream()
            .reduce(JobStatus.FINISHED, (jobStatus, task) -> {
                    if (task.getDesiredJobStatus().before(jobStatus)) {
                        jobStatus = task.getDesiredJobStatus();
                    }
                    return jobStatus;
                }
                , (js1, js2) -> js1.before(js2) ? js1 : js2);
        return desiredJobStatuses;
    }

}
