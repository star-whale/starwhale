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

package ai.starwhale.mlops.schedule.impl.k8s;

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.CoreV1Event;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ResourceEventHolder implements ResourceEventHandler<CoreV1Event> {
    private final long eventTtlSec;
    // map.of(nameOfRelatedResource, map.of(nameOfEvent, Event))
    private final Map<String, Map<String, Event>> events;
    // TODO: deal with ReplicaSet
    private final Set<String> allowedResources = Set.of("Pod", "Deployment", "Job", "Statefulset");

    /**
     * ResourceEventHolder holds events from k8s
     * The event-ttl is 1 hour by default see <a href="https://github.com/kubernetes/kubernetes/blob/d2f40481d115155f3ce7abc0b7b2ff2cf8a0bb1e/cmd/kube-apiserver/app/options/options.go#L111">detail</a>
     * We will hold the events may be deleted in the k8s api server
     */
    public ResourceEventHolder(
            @Value("${sw.infra.k8s.event-holder-ttl-in-seconds}") long eventTtlSec
    ) {
        this.eventTtlSec = eventTtlSec;
        events = new ConcurrentHashMap<>();
    }

    /**
     * returns the event list ordered by event time ascending
     *
     * @param kind resource type, support Pod, Deployment, Job, Statefulset for now
     * @param name resource name, e.g. foo-bar-123
     * @return event list
     */
    public List<Event> getEvents(String kind, String name) {
        var entry = events.get(Event.objectName(kind, name));
        if (entry == null) {
            return List.of();
        }
        return getOrderedEvents(entry);
    }

    public List<Event> getPodEvents(String name) {
        return getEvents("Pod", name);
    }

    private List<Event> getOrderedEvents(Map<String, Event> entry) {
        return entry.values().stream()
                .sorted(Comparator.comparingLong(Event::getEventTimeInMs))
                .collect(Collectors.toList());
    }

    @Override
    public void onAdd(CoreV1Event coreV1Event) {
        var event = Event.fromCoreV1Event(coreV1Event);
        if (!allowedResources.contains(coreV1Event.getInvolvedObject().getKind())) {
            log.info("Ignoring event: {}", event);
            return;
        }

        var rc = events.computeIfAbsent(event.getObject(), k -> new ConcurrentHashMap<>());
        rc.put(event.getName(), event);
        log.info("Event added: {}", event);
    }

    @Override
    public void onUpdate(CoreV1Event coreV1Event, CoreV1Event apiType1) {
        // no update
    }

    @Override
    public void onDelete(CoreV1Event coreV1Event, boolean b) {
        // no deletion
    }

    @Scheduled(timeUnit = TimeUnit.MINUTES, initialDelay = 10, fixedDelay = 10)
    public void gc() {
        events.entrySet().removeIf(e -> {
            var list = getOrderedEvents(e.getValue());
            var last = list.get(list.size() - 1);
            var expireTime = last.getEventTimeInMs() + TimeUnit.MILLISECONDS.convert(eventTtlSec, TimeUnit.SECONDS);
            var expired = expireTime < System.currentTimeMillis();
            if (expired) {
                log.info("remove expired events for {}", e.getKey());
            }
            return expired;
        });
    }

    @Builder
    @Data
    public static class Event {
        // the name of the event, used for logging
        final String name;
        // the event time in milliseconds (uses the first event time when the event occurs multiple times)
        final Long eventTimeInMs;
        // events will be deduplicated by k8s, count means the number of the same events
        // https://github.com/kubernetes/kubectl/blob/c230e1b22140e5767a57860f8d1ca3669859565d/pkg/describe/describe.go#L4211-L4214
        final Integer count;
        // staging/src/k8s.io/api/core/v1/types.go
        // one of (Normal, Warning) for now
        final String type;
        // main parts of the event reasons: https://github.com/kubernetes/kubernetes/blob/master/pkg/kubelet/events/event.go
        final String reason;
        // the detail message, usually a long sentence
        final String message;
        // the related objects resource name, e.g. pod/foo-bar-12345
        final String object;

        public static Event fromCoreV1Event(CoreV1Event coreV1Event) {
            Long tm = null;
            // https://github.com/kubernetes/kubectl/blob/c230e1b22140e5767a57860f8d1ca3669859565d/pkg/describe/describe.go#L4196-L4230
            var eventTime = coreV1Event.getEventTime();
            if (eventTime == null) {
                var firstTime = coreV1Event.getFirstTimestamp();
                if (firstTime == null) {
                    log.warn("Event time is null, ignore event: {}", coreV1Event);
                } else {
                    // note that the timestamp may in second level
                    // we may get the timestamp like 1676268792000
                    // see: https://stackoverflow.com/questions/67523035
                    tm = firstTime.toInstant().toEpochMilli();
                }
            } else {
                tm = eventTime.toInstant().toEpochMilli();
            }
            var count = 0;
            if (coreV1Event.getSeries() != null && coreV1Event.getSeries().getCount() != null) {
                count = coreV1Event.getSeries().getCount();
            } else if (coreV1Event.getCount() != null) {
                count = coreV1Event.getCount();
            }
            return Event.builder()
                    .name(coreV1Event.getMetadata().getName())
                    .eventTimeInMs(tm)
                    .count(count)
                    .type(coreV1Event.getType())
                    .reason(coreV1Event.getReason())
                    .message(coreV1Event.getMessage())
                    .object(objectName(coreV1Event.getInvolvedObject().getKind(),
                            coreV1Event.getInvolvedObject().getName()))
                    .build();
        }

        public static String objectName(String kind, String name) {
            return kind + "/" + name;
        }
    }
}
