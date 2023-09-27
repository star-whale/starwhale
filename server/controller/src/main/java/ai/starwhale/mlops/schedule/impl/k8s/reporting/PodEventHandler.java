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

package ai.starwhale.mlops.schedule.impl.k8s.reporting;

import ai.starwhale.mlops.domain.run.bo.RunStatus;
import ai.starwhale.mlops.schedule.impl.k8s.Util;
import ai.starwhale.mlops.schedule.reporting.ReportedRun;
import ai.starwhale.mlops.schedule.reporting.RunReportReceiver;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@ConditionalOnProperty(value = "sw.scheduler.impl", havingValue = "k8s")
public class PodEventHandler implements ResourceEventHandler<V1Pod> {

    final RunReportReceiver runReportReceiver;

    public PodEventHandler(RunReportReceiver runReportReceiver) {
        this.runReportReceiver = runReportReceiver;
    }

    @Override
    public void onAdd(V1Pod obj) {
    }

    @Override
    public void onUpdate(V1Pod oldObj, V1Pod newObj) {
        reportRunStatus(newObj);
    }

    @Override
    public void onDelete(V1Pod obj, boolean deletedFinalStateUnknown) {
    }

    private Long getJobNameAsId(V1Pod pod) {
        String jobName = pod.getMetadata().getLabels().get("job-name");
        if (null == jobName || jobName.isBlank()) {
            log.info("no id found for pod {}", jobName);
            return null;
        }
        Long id;
        try {
            id = Long.valueOf(jobName);
        } catch (Exception e) {
            log.warn("id is not number {}", jobName);
            id = null;
        }
        return id;
    }

    private void reportRunStatus(V1Pod pod) {
        if (null == pod.getStatus() || null == pod.getStatus().getPhase()) {
            return;
        }

        // https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-termination
        if (pod.getMetadata() != null && pod.getMetadata().getDeletionTimestamp() != null) {
            log.info("pod {} is being deleted", pod.getMetadata().getName());
            return;
        }
        Long rid = getJobNameAsId(pod);
        if (rid == null) {
            return;
        }

        RunStatus runStatus;
        var phase = pod.getStatus().getPhase();
        if (StringUtils.hasText(phase)) {
            switch (phase) {
                /*
                Pending The Pod has been accepted by the Kubernetes cluster,
                but one or more of the containers has not been set up and made ready to run.
                This includes time a Pod spends waiting to be scheduled
                as well as the time spent downloading container images over the network.
                 */
                case "Pending":
                    runStatus = RunStatus.PENDING;
                    break;
                /*
                Running The Pod has been bound to a node, and all the containers have been created.
                At least one container is still running, or is in the process of starting or restarting.
                 */
                case "Running":
                    runStatus = RunStatus.RUNNING;
                    break;
                default:
                    return;

            }
        } else {
            return;
        }

        Long startTime = null;
        if (pod.getStatus() != null) {
            startTime = Util.k8sTimeToMs(pod.getStatus().getStartTime());
        }
        log.debug("run:{} status changed to {}.", rid, runStatus);
        var report = ReportedRun.builder()
                .id(rid)
                .status(runStatus)
                .ip(pod.getStatus().getPodIP())
                .startTimeMillis(startTime)
                .stopTimeMillis(null)
                .build();
        runReportReceiver.receive(report);
    }

}
