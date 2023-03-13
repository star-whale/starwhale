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

package ai.starwhale.mlops.domain.upgrade.step;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.domain.upgrade.UpgradeAccess;
import ai.starwhale.mlops.domain.upgrade.bo.Upgrade;
import ai.starwhale.mlops.domain.upgrade.bo.Upgrade.STATUS;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class UpgradeStepTest {

    private UpgradeStepManager manager;
    private BackupDatabase backupDatabase;
    private UpdateK8sImage updateK8sImage;

    private List<UpgradeStep> steps;

    private UpgradeAccess access;
    private K8sClient k8sClient;

    private ThreadPoolTaskScheduler scheduler;

    private ScheduledFuture<?> future;

    private AtomicBoolean deployed = new AtomicBoolean();
    private AtomicBoolean completed = new AtomicBoolean();


    @BeforeEach
    public void setUp() throws Exception {
        access = mock(UpgradeAccess.class);
        k8sClient = mock(K8sClient.class);
        deployed.set(false);
        Mockito.doAnswer(in -> {
                    deployed.set(true);
                    return null;
                })
                .when(k8sClient)
                .patchDeployment(anyString(), any(), anyString());
        Mockito.when(k8sClient.getNotReadyPods(anyString()))
                .thenAnswer((Answer<List<V1Pod>>) invocation -> {
                    if (deployed.get()) {
                        return List.of();
                    } else {
                        return List.of(new V1Pod());
                    }
                });
        backupDatabase = new BackupDatabase(access);
        updateK8sImage = new UpdateK8sImage(access, k8sClient);
        steps = List.of(backupDatabase, updateK8sImage);
        scheduler = mock(ThreadPoolTaskScheduler.class);
        future = new ScheduledFuture<>() {
            @Override
            public long getDelay(@NotNull TimeUnit unit) {
                return 0;
            }

            @Override
            public int compareTo(@NotNull Delayed o) {
                return 0;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return completed.getAndSet(true);
            }

            @Override
            public boolean isCancelled() {
                return completed.get();
            }

            @Override
            public boolean isDone() {
                return completed.get();
            }

            @Override
            public Object get() {
                return null;
            }

            @Override
            public Object get(long timeout, @NotNull TimeUnit unit) {
                return null;
            }
        };
        Mockito.doAnswer(invocation -> {
                    StepTask task = invocation.getArgument(0);
                    task.setFuture(future);
                    for (int i = 0; i < steps.size(); i++) {
                        task.run();
                    }
                    return future;
                })
                .when(scheduler)
                .schedule(any(StepTask.class), any(Trigger.class));
        completed.set(false);
        manager = new UpgradeStepManager(steps, scheduler);
    }

    @Test
    public void testBackupDatabase() {
        Upgrade upgrade = new Upgrade("pid", "0.2", "server:0.2", "0.1", "server:0.1", STATUS.UPGRADING);
        backupDatabase.doStep(upgrade);
    }

    @Test
    public void testUpdateK8sImage() {
        Upgrade upgrade = new Upgrade("pid", "0.2", "server:0.2", "0.1", "server:0.1", STATUS.UPGRADING);
        updateK8sImage.doStep(upgrade);
        Assertions.assertTrue(updateK8sImage.isComplete());
    }

    @Test
    public void testStepTask() {
        Upgrade upgrade = new Upgrade("pid", "0.2", "server:0.2", "0.1", "server:0.1", STATUS.UPGRADING);
        StepTask stepTask = new StepTask(upgrade, steps);
        stepTask.setFuture(future);
        for (int i = 0; i < steps.size(); i++) {
            stepTask.run();
        }
        Assertions.assertTrue(deployed.get());
        Assertions.assertTrue(completed.get());
    }

    @Test
    public void testManager() {
        Upgrade upgrade = new Upgrade("pid", "0.2", "server:0.2", "0.1", "server:0.1", STATUS.UPGRADING);
        manager.runSteps(upgrade);
        Assertions.assertTrue(deployed.get());
        Assertions.assertTrue(completed.get());
    }

}
