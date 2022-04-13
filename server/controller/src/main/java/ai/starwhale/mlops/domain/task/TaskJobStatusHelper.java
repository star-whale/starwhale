package ai.starwhale.mlops.domain.task;

import ai.starwhale.mlops.domain.job.Job.JobStatus;
import ai.starwhale.mlops.domain.task.bo.Task;
import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class TaskJobStatusHelper {

    public JobStatus desiredJobStatus(Collection<Task> tasks){
        final JobStatus desiredJobStatuses = tasks.parallelStream()
            .map(task -> task.getStatus())
            .reduce(JobStatus.FINISHED, (jobStatus, taskStatus) -> {
                    if (taskStatus.getDesiredJobStatus().before(jobStatus)) {
                        jobStatus = taskStatus.getDesiredJobStatus();
                    }
                    return jobStatus;
                }
                , (js1, js2) -> js1.before(js2) ? js1 : js2);
        return desiredJobStatuses;
    }

}
