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

package ai.starwhale.mlops.schedule.reporting.listener.impl;

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.schedule.executor.RunExecutor;
import ai.starwhale.mlops.schedule.log.RunLogSaver;
import ai.starwhale.mlops.schedule.reporting.listener.RunUpdateListener;
import java.time.Instant;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RunUpdateListenerForGc implements RunUpdateListener {

    final RunLogSaver runLogSaver;
    final DelayQueue<RunToBeDeleted> runToDeletes;

    final Long deletionDelayMilliseconds;

    final RunExecutor runExecutor;


    public RunUpdateListenerForGc(
            RunLogSaver runLogSaver,
            @Value("${sw.task.deletion-delay-minutes}") Long deletionDelayMinutes,
            RunExecutor runExecutor
    ) {
        this.runLogSaver = runLogSaver;
        this.runExecutor = runExecutor;
        this.runToDeletes = new DelayQueue<>();
        this.deletionDelayMilliseconds = TimeUnit.MILLISECONDS.convert(deletionDelayMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void onRunUpdate(Run run) {
        RunStatus runStatus = run.getStatus();
        if (runStatus == RunStatus.FAILED || runStatus == RunStatus.FINISHED) {
            runLogSaver.saveLog(run);
            if (deletionDelayMilliseconds <= 0) {
                runExecutor.remove(run);
                log.debug("delete run {}", run.getId());
                return;
            }
            //delay delete
            addToDeleteQueue(run);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Scheduled(fixedDelay = 30000)
    public void processTaskDeletion() {
        var toDelete = runToDeletes.poll();
        while (toDelete != null) {
            runExecutor.remove(toDelete.getRun());
            log.debug("delete run {}", toDelete.getRun().getId());
            toDelete = runToDeletes.poll();
        }
    }

    private void addToDeleteQueue(Run run) {
        long start = Instant.now().toEpochMilli();
        if (null != run.getFinishTime()) {
            start = run.getFinishTime();
        }
        var deleteTime = start + deletionDelayMilliseconds;
        runToDeletes.put(new RunToBeDeleted(run, deleteTime));
        log.debug("add run {} to delete queue, delete time {}", run.getId(), deleteTime);
    }

    static class RunToBeDeleted implements Delayed {

        private final Run run;

        private final Long deleteTime;

        public RunToBeDeleted(Run run, Long deleteTime) {
            this.run = run;
            this.deleteTime = deleteTime;
        }

        public Run getRun() {
            return run;
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            return unit.convert(deleteTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NotNull Delayed o) {
            long diffMillis = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
            diffMillis = Math.min(diffMillis, 1);
            diffMillis = Math.max(diffMillis, -1);
            return (int) diffMillis;
        }
    }
}
