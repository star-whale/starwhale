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

package ai.starwhale.mlops.domain.upgrade;

import ai.starwhale.mlops.domain.job.JobService;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.lock.ControllerLock;
import ai.starwhale.mlops.domain.upgrade.bo.Upgrade;
import ai.starwhale.mlops.domain.upgrade.bo.Upgrade.STATUS;
import ai.starwhale.mlops.domain.upgrade.bo.UpgradeLog;
import ai.starwhale.mlops.domain.upgrade.step.UpgradeStepManager;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpgradeService {

    public static final String LABEL_CONTROLLER = "starwhale.ai/role=controller";
    private final UpgradeAccess upgradeAccess;
    private final ControllerLock controllerLock;
    private final JobService jobService;
    private final K8sClient k8sClient;
    private final UpgradeStepManager upgradeStepManager;
    private final String currentVersionNumber;
    private static final String LOCK_OPERATOR = "upgrade";
    private final AtomicReference<Upgrade> upgradeAtomicReference;


    public UpgradeService(UpgradeAccess upgradeAccess,
            ControllerLock controllerLock,
            JobService jobService,
            K8sClient k8sClient,
            UpgradeStepManager upgradeStepManager,
            @Value("${sw.version}") String starwhaleVersion) {
        this.upgradeAccess = upgradeAccess;
        this.controllerLock = controllerLock;
        this.jobService = jobService;
        this.k8sClient = k8sClient;
        this.currentVersionNumber = StrUtil.subBefore(starwhaleVersion, ":", false);
        this.upgradeAtomicReference = new AtomicReference<>();
        this.upgradeStepManager = upgradeStepManager;
    }

    public Upgrade upgrade(String version, String image) {
        // 0. lock controller writing request
        controllerLock.lock(ControllerLock.TYPE_WRITE_REQUEST, LOCK_OPERATOR);

        try {
            // 1. Check whether upgrade is allowed
            checkIsUpgradeAllowed(version);

            // 2. Set server status to upgrading
            String progressId = buildUuid();
            upgradeAccess.setStatusToUpgrading(progressId);

            Upgrade upgrade = new Upgrade(progressId,
                    version,
                    image,
                    currentVersionNumber,
                    getCurrentImage(),
                    STATUS.UPGRADING);
            doUpgrade(upgrade);

            return upgrade;
        } catch (Exception e) {
            controllerLock.unlock(ControllerLock.TYPE_WRITE_REQUEST, LOCK_OPERATOR);
            throw e;
        }
    }

    public List<UpgradeLog> getUpgradeLog() {
        if (upgradeAtomicReference.get() == null) {
            return List.of();
        }
        String progressId = upgradeAtomicReference.get().getProgressId();
        return upgradeAccess.readLog(progressId);
    }

    public void cancelUpgrade() {
        if (upgradeAtomicReference.get() == null) {
            throw new SwValidationException(ValidSubject.UPGRADE, "No upgrade progress is running");
        }
        checkIsCancelUpgradeAllowed();

        doCancel(upgradeAtomicReference.get());
        // 1. unlock requests
        controllerLock.unlock(ControllerLock.TYPE_WRITE_REQUEST, LOCK_OPERATOR);

    }

    private String getCurrentImage() {
        try {
            V1DeploymentList deployment = k8sClient.listDeployment(LABEL_CONTROLLER);
            if (deployment.getItems().isEmpty()) {
                throw new SwProcessException(ErrorType.K8S, "Can't get the deployment of controller");
            }
            V1DeploymentSpec deploymentSpec = Objects.requireNonNull(deployment.getItems().get(0).getSpec());
            V1PodSpec podSpec = Objects.requireNonNull(deploymentSpec.getTemplate().getSpec());

            return podSpec.getContainers().get(0).getImage();

        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.K8S, "K8sClient Error", e);
        }
    }

    private void checkIsUpgradeAllowed(String newVersion) {
        // Check whether upgrade is allowed:
        // 0. new version is later than current version
        if (StrUtil.compareVersion(newVersion, currentVersionNumber) <= 0) {
            throw new SwValidationException(ValidSubject.UPGRADE, "New version must be later than the current version");
        }
        // 1. all Pods are ready (No upgrade process is running)
        try {

            List<V1Pod> notReadyPods = k8sClient.getNotReadyPods(LABEL_CONTROLLER);
            if (!notReadyPods.isEmpty()) {
                throw new SwValidationException(ValidSubject.UPGRADE, "Pods are not all ready.");
            }
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.K8S, "K8sClient Error", e);
        }

        // 2. no hot job is remaining
        List<Job> jobs = jobService.listHotJobs();
        if (jobs.size() > 0) {
            throw new SwValidationException(ValidSubject.UPGRADE, "There are still remaining hot jobs.");
        }

        //TODO 3. terminate datastore

    }

    private void checkIsCancelUpgradeAllowed() {
        // Check whether cancel upgrade is allowed:
        // 1. New Pod is not ready
        // 2. Pulling image or New Pod has restarted more than 3 times
    }

    private String buildUuid() {
        // Build upgrade progress uuid. It Contains current server version.
        return String.format("%s_%s", currentVersionNumber, IdUtil.simpleUUID());
    }

    private void doUpgrade(Upgrade upgrade) {
        log.info("Upgrading");
        upgradeAtomicReference.set(upgrade);
        try {
            upgradeStepManager.runSteps(upgrade);
        } catch (Exception e) {
            log.error("", e);
            upgradeAccess.setStatusToNormal();
        }
    }

    private void doCancel(Upgrade upgrade) {
        log.info("The upgrade progress is cancelled.");

        upgrade.setStatus(STATUS.CANCELLING);
        upgradeAccess.setStatusToNormal();
        upgrade.setStatus(STATUS.CANCELED);
    }
}
