package ai.starwhale.mlops.configuration.schedule;

import org.springframework.boot.task.TaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;

public class ScheduleConfig {

    /**
     * when system receive kill signal when there are scheduled tasks running, system should wait for running tasks to be done to exit
     * @return
     */
    @Bean
    TaskSchedulerCustomizer taskSchedulerCustomizer() {
        return taskScheduler -> {
            taskScheduler.setAwaitTerminationSeconds(60);
            taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        };
    }

}
