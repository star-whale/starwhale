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

package ai.starwhale.mlops.schedule.k8s;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1Status;
import java.io.IOException;
import java.util.List;
import lombok.SneakyThrows;
import okhttp3.Call;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sw.infra.k8s.enabled", havingValue = "false")
public class K8sClientDummy implements K8sClient {
    private final String dummyMessage = "Dummy K8s client is used. "
            + "Please enable K8sClient by setting SW_INFRA_K8S_ENABLED=true";
    private final ApiException dummyException = new ApiException(dummyMessage);

    @Override
    public V1Job deployJob(V1Job job) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1StatefulSet deployStatefulSet(V1StatefulSet ss) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1Service deployService(V1Service svc) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1Secret createSecret(V1Secret secret) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1Secret replaceSecret(String name, V1Secret secret) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1Status deleteSecret(String name) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1Secret getSecret(String name) throws ApiException {
        throw dummyException;
    }

    @Override
    public void deleteJob(String name) throws ApiException {
        throw dummyException;
    }

    @Override
    public void deleteStatefulSet(String name) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1Deployment patchDeployment(String deploymentName, V1Patch patch, String patchFormat) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1DeploymentList listDeployment(String labelSelector) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1JobList getJobs(String labelSelector) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1StatefulSetList getStatefulSetList(String labelSelector) throws ApiException {
        throw dummyException;
    }

    @Override
    public V1PodList getPodList(String labelSelector) throws ApiException {
        throw dummyException;
    }

    @Override
    public String[] execInPod(String podName, String containerName, String... command) throws ApiException {
        throw dummyException;
    }

    @Override
    public List<V1Pod> getNotReadyPods(String labelSelector) throws ApiException {
        throw dummyException;
    }

    @Override
    public void watchJob(ResourceEventHandler<V1Job> eventH, String selector) {
    }

    @Override
    public void watchPod(ResourceEventHandler<V1Pod> eventH, String selector) {
    }

    @Override
    public void watchStatefulSet(ResourceEventHandler<V1StatefulSet> eventH, String selector) {
    }

    @Override
    public void watchEvent(ResourceEventHandler<CoreV1Event> eventH, String selector) {
    }

    @Override
    public String logOfJob(String selector, List<String> containers) throws ApiException, IOException {
        throw dummyException;
    }

    @SneakyThrows
    @NotNull
    @Override
    public String logOfPod(V1Pod pod, List<String> containers) {
        throw dummyException;
    }

    @Nullable
    @Override
    public V1Pod podOfJob(String selector) throws ApiException {
        throw dummyException;
    }

    @Override
    public Call readLog(String pod, String container, boolean follow) throws IOException, ApiException {
        throw dummyException;
    }

    @Override
    public void watchNode(ResourceEventHandler<V1Node> eventHandlerNode) {

    }

    @Override
    public V1PodList getPodsByJobName(String job) throws ApiException {
        throw dummyException;
    }

    @Override
    public List<V1Pod> getPodsByJobNameQuietly(String jobId) {
        return null;
    }
}
