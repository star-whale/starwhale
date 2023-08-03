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

package ai.starwhale.mlops.schedule.impl.k8s.log;

import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.log.TaskLogStreamingCollector;
import io.kubernetes.client.openapi.ApiException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import okhttp3.Call;
import okhttp3.Response;

public class TaskLogK8SStreamingCollector implements TaskLogStreamingCollector {
    public static final String WORKER_CONTAINER = "worker";
    final K8sClient k8sClient;
    final Call call;
    final Response resp;
    final BufferedReader bufferedReader;

    public TaskLogK8SStreamingCollector(K8sClient k8sClient, String jobName)
            throws IOException, ApiException {
        this.k8sClient = k8sClient;
        call = k8sClient.readLog(getPodName(jobName), WORKER_CONTAINER, true);
        resp = call.execute();
        bufferedReader = new BufferedReader(new InputStreamReader(resp.body().byteStream(), StandardCharsets.UTF_8));
    }

    private String getPodName(String taskId) throws ApiException {
        var podList = k8sClient.getPodsByJobName(taskId);
        if (podList.getItems().isEmpty()) {
            throw new ApiException("get empty pod list by job name " + taskId);
        }
        // returns the running pod
        var thePod = podList.getItems().stream().filter(pod -> {
            if (pod.getStatus() == null) {
                return false;
            }
            if (pod.getStatus().getPhase() == null) {
                return false;
            }
            return pod.getStatus().getPhase().equals("Running");
        }).findFirst();
        if (thePod.isEmpty()) {
            throw new ApiException("get empty running pod by job name " + taskId);
        }
        return thePod.get().getMetadata().getName();
    }

    @Override
    public String readLine() throws IOException {
        return bufferedReader.readLine();
    }

    @Override
    public void cancel() {
        resp.close();
        call.cancel();
    }
}
