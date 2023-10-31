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

import ai.starwhale.mlops.api.protocol.event.Event.EventResourceType;
import ai.starwhale.mlops.api.protocol.event.Event.EventSource;
import ai.starwhale.mlops.api.protocol.event.Event.EventType;
import ai.starwhale.mlops.api.protocol.event.Event.RelatedResource;
import ai.starwhale.mlops.api.protocol.event.EventRequest;
import ai.starwhale.mlops.domain.event.EventService;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import ai.starwhale.mlops.schedule.impl.k8s.RunExecutorK8s;
import ai.starwhale.mlops.schedule.impl.k8s.Util;
import ai.starwhale.mlops.schedule.impl.k8s.reporting.ResourceEventHolder.Event;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@ConditionalOnExpression("${sw.job.event.scheduler-event-log.enabled} and '${sw.scheduler.impl}'.equals('k8s')")
public class RunEventListener implements K8sEventListener {
    private final EventService eventService;
    private final K8sClient k8sClient;

    /**
     * @see RunExecutorK8s.JOB_NAME_FORMATTER
     */
    private static final Pattern STARWHALE_WORKLOAD_POD_NAME_PATTERN = Pattern.compile("^starwhale-run-\\d+-.*");
    private static final int MAX_MESSAGE_LENGTH = 255;


    public RunEventListener(EventService eventService, K8sClient k8sClient) {
        this.eventService = eventService;
        this.k8sClient = k8sClient;
    }

    @Override
    public void onEvent(Event event) {
        // we only care about run events of pod
        if (!event.getKind().equals("Pod")) {
            return;
        }
        if (!STARWHALE_WORKLOAD_POD_NAME_PATTERN.matcher(event.getName()).matches()) {
            return;
        }

        V1Pod pod;
        try {
            pod = k8sClient.getPod(event.getName());
        } catch (ApiException e) {
            log.warn("failed to get pod {} for event {}", event.getName(), event);
            return;
        }

        var runId = Util.getRunId(pod);
        if (runId == null) {
            return;
        }

        eventService.addEvent(
                EventRequest.builder()
                        .eventType(getType(event))
                        .message(getReason(event))
                        .source(EventSource.NODE)
                        .relatedResource(new RelatedResource(EventResourceType.RUN, runId))
                        .build()
        );
    }

    private static EventType getType(Event event) {
        return event.getType().equals("Warning") ? EventType.WARNING : EventType.INFO;
    }

    private static String getReason(Event event) {
        var ret = Stream.of(event.getReason(), event.getMessage())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));

        if (ret.length() > MAX_MESSAGE_LENGTH) {
            ret = ret.substring(0, MAX_MESSAGE_LENGTH);
        }
        return ret;
    }
}
