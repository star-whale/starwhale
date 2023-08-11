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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.mock;

import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.domain.job.JobService;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.lock.ControllerLock;
import ai.starwhale.mlops.domain.lock.ControllerLockImpl;
import ai.starwhale.mlops.domain.upgrade.bo.UpgradeLog;
import ai.starwhale.mlops.domain.upgrade.bo.Version;
import ai.starwhale.mlops.domain.upgrade.step.UpgradeStepManager;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

public class UpgradeServiceTest {

    private UpgradeService upgradeService;

    private UpgradeAccess upgradeAccess;
    private ControllerLock controllerLock;
    private JobService jobService;
    private DataStore dataStore;
    private K8sClient k8sClient;
    private UpgradeStepManager upgradeStepManager;

    private RestTemplate restTemplate;
    private String currentVersionNumber;

    private String latestVersionApiUrl;


    @BeforeEach
    public void setUp() throws Exception {
        upgradeAccess = mock(UpgradeAccess.class);
        Mockito.when(upgradeAccess.readLog(anyString()))
                .thenReturn(List.of(UpgradeLog.builder().build()));
        controllerLock = new ControllerLockImpl();
        jobService = mock(JobService.class);
        dataStore = mock(DataStore.class);
        k8sClient = mock(K8sClient.class);

        V1Container container = new V1Container();
        container.setImage("server:0.4.0");
        V1PodSpec podSpec = new V1PodSpec();
        podSpec.addContainersItem(container);
        V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
        templateSpec.setSpec(podSpec);
        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec();
        deploymentSpec.setTemplate(templateSpec);
        V1Deployment deploy = new V1Deployment();
        deploy.setSpec(deploymentSpec);
        V1DeploymentList deployments = new V1DeploymentList();
        deployments.addItemsItem(deploy);

        Mockito.when(k8sClient.listDeployment(anyString()))
                .thenReturn(deployments);
        upgradeStepManager = mock(UpgradeStepManager.class);
        restTemplate = mock(RestTemplate.class);
        currentVersionNumber = "0.4.0";
        latestVersionApiUrl = "";

        upgradeService = new UpgradeService(upgradeAccess, controllerLock, jobService, dataStore,
                k8sClient, upgradeStepManager, restTemplate, currentVersionNumber, latestVersionApiUrl);
    }

    @Test
    public void testUpgrade() throws Exception {
        var log = upgradeService.getUpgradeLog();
        Assertions.assertEquals(0, log.size());

        assertThrows(SwValidationException.class, () -> {
            upgradeService.upgrade(new Version("0.3.5", "server:0.3.5"));
        });
        Assertions.assertFalse(controllerLock.isLocked(ControllerLock.TYPE_WRITE_REQUEST));

        Mockito.when(k8sClient.getNotReadyPods(anyString()))
                .thenReturn(List.of(new V1Pod()));

        assertThrows(SwValidationException.class, () -> {
            upgradeService.upgrade(new Version("0.4.1", "server:0.4.1"));
        });
        Assertions.assertFalse(controllerLock.isLocked(ControllerLock.TYPE_WRITE_REQUEST));

        Mockito.when(k8sClient.getNotReadyPods(anyString()))
                .thenReturn(List.of());
        Mockito.when(jobService.listHotJobs())
                .thenReturn(List.of(new Job()));

        assertThrows(SwValidationException.class, () -> {
            upgradeService.upgrade(new Version("0.4.1", "server:0.4.1"));
        });
        Assertions.assertFalse(controllerLock.isLocked(ControllerLock.TYPE_WRITE_REQUEST));

        Mockito.when(jobService.listHotJobs())
                .thenReturn(List.of());

        upgradeService.upgrade(new Version("0.4.1", "server:0.4.1"));
        Assertions.assertTrue(controllerLock.isLocked(ControllerLock.TYPE_WRITE_REQUEST));

        log = upgradeService.getUpgradeLog();
        Assertions.assertEquals(1, log.size());

        upgradeService.cancelUpgrade();
        Assertions.assertFalse(controllerLock.isLocked(ControllerLock.TYPE_WRITE_REQUEST));
    }
}
