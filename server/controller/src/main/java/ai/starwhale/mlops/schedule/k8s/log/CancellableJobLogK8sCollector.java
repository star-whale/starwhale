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

package ai.starwhale.mlops.schedule.k8s.log;

import ai.starwhale.mlops.schedule.k8s.K8sClient;
import io.kubernetes.client.openapi.ApiException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import okhttp3.Call;
import okhttp3.Response;

public class CancellableJobLogK8sCollector implements CancellableJobLogCollector {
    public static final String WORKER_CONTAINER = "worker";
    final K8sClient k8sClient;
    final Call call;
    Response resp;
    BufferedReader bufferedReader;

    public CancellableJobLogK8sCollector(K8sClient k8sClient, String jobName)
            throws IOException, ApiException {
        this.k8sClient = k8sClient;
        call = k8sClient.readLog(getPodName(jobName), WORKER_CONTAINER, true);
    }

    private String getPodName(String taskId) throws ApiException {
        var podList = k8sClient.getPodsByJobName(taskId);
        if (podList.getItems().isEmpty()) {
            throw new ApiException("get empty pod list by job name " + taskId);
        }
        return podList.getItems().get(0).getMetadata().getName();
    }

    @Override
    public String readLine() throws IOException {
        if (bufferedReader == null) {
            resp = call.execute();
            if (resp.isSuccessful()) {
                bufferedReader = new BufferedReader(
                        new InputStreamReader(resp.body().byteStream(), StandardCharsets.UTF_8));
            } else {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return resp.message();
            }
        }
        return bufferedReader.readLine();
    }

    @Override
    public void cancel() {
        if (null != call) {
            resp.close();
        }
        if (null != call) {
            call.cancel();
        }
    }
}
