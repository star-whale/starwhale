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

package ai.starwhale.mlops.domain.event;

import ai.starwhale.mlops.api.protocol.event.Event;
import ai.starwhale.mlops.api.protocol.event.Event.EventResourceType;
import ai.starwhale.mlops.api.protocol.event.Event.EventSource;
import ai.starwhale.mlops.api.protocol.event.Event.EventType;
import ai.starwhale.mlops.api.protocol.event.Event.RelatedResource;
import ai.starwhale.mlops.api.protocol.event.EventRequest;
import ai.starwhale.mlops.api.protocol.event.EventVo;
import ai.starwhale.mlops.domain.event.mapper.EventMapper;
import ai.starwhale.mlops.domain.event.po.EventEntity;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.run.RunEntity;
import ai.starwhale.mlops.domain.run.mapper.RunMapper;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentLruCache;

@Slf4j
@Service
public class EventService {
    private final ConcurrentLruCache<Long, Long> taskToJobMap;
    private final ConcurrentLruCache<Long, Long> runToJobMap;

    private final TaskMapper taskMapper;
    private final StepMapper stepMapper;
    private final RunMapper runMapper;
    private final EventMapper eventMapper;
    private final EventConverter eventConverter;
    private final JobDao jobDao;

    private static final long INVALID_JOB_ID = -1L;

    public EventService(
            TaskMapper taskMapper,
            StepMapper stepMapper,
            RunMapper runMapper,
            EventMapper eventMapper,
            EventConverter eventConverter,
            JobDao jobDao,
            @Value("${sw.job.event.task-to-job-cache-capacity}") int taskToJobCacheSizeLimit,
            @Value("${sw.job.event.run-to-job-cache-capacity}") int runToJobCacheSizeLimit
    ) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.runMapper = runMapper;
        this.eventMapper = eventMapper;
        this.eventConverter = eventConverter;
        this.jobDao = jobDao;
        this.taskToJobMap = new ConcurrentLruCache<>(taskToJobCacheSizeLimit, this::getJobIdByTask);
        this.runToJobMap = new ConcurrentLruCache<>(runToJobCacheSizeLimit, this::getJobIdByRun);
    }

    public void addEvent(EventRequest event) {
        var eventEntity = eventConverter.toEntity(event);
        if (eventEntity.getCreatedTime() == null) {
            eventEntity.setCreatedTime(new Date());
        }
        eventMapper.insert(eventEntity);
    }

    @NotNull
    public List<EventVo> getEvents(EventRequest.RelatedResource related) {
        List<EventEntity> events = new LinkedList<>();

        // returns all the run events belongs to the task when the scope is task.
        // we do not support job scope aggregation for now.
        if (related.getEventResourceType() == EventResourceType.TASK) {
            var runs = runMapper.list(related.getId());
            if (runs == null || runs.isEmpty()) {
                return List.of();
            }
            var runIds = runs.stream().map(RunEntity::getId).collect(Collectors.toList());
            var runEvents = eventMapper.listEventsOfResources(EventResourceType.RUN, runIds);
            if (runEvents != null && !runEvents.isEmpty()) {
                events.addAll(runEvents);
            }
        }

        var resourceEvents = eventMapper.listEventsOfResource(related.getEventResourceType(), related.getId());
        if (resourceEvents != null && !resourceEvents.isEmpty()) {
            events.addAll(resourceEvents);
        }

        if (events.isEmpty()) {
            return List.of();
        }

        // order by created time asc
        return events.stream()
                .map(eventConverter::toVo)
                .sorted(Comparator.comparingLong(Event::getTimestamp))
                .collect(Collectors.toList());
    }

    public void addEventForJob(String jobUrl, @NotNull EventRequest event) {
        var jobId = jobDao.getJobId(jobUrl);
        validateOwnership(jobId, event.getRelatedResource());
        addEvent(event);
    }

    public List<EventVo> getEventsForJob(String jobUrl, @Nullable EventRequest.RelatedResource related) {
        var jobId = jobDao.getJobId(jobUrl);
        validateOwnership(jobId, related);
        if (related == null) {
            related = new EventRequest.RelatedResource();
            related.setEventResourceType(EventResourceType.JOB);
            related.setId(jobId);
        }
        return getEvents(related);
    }

    public void addInternalJobInfoEvent(Long jobId, String message) {
        addEvent(EventRequest.builder()
                .eventType(EventType.INFO)
                .source(EventSource.SERVER)
                .relatedResource(new RelatedResource(EventResourceType.JOB, jobId))
                .message(message)
                .build());
    }

    private void validateOwnership(Long jobId, EventRequest.RelatedResource related) {
        if (!resourceBelongsToTheJob(jobId, related)) {
            throw new SwNotFoundException(ResourceType.BUNDLE, "related resource is not found");
        }
    }

    private boolean resourceBelongsToTheJob(Long jobId, @Nullable EventRequest.RelatedResource related) {
        if (related == null) {
            return true;
        }

        Long actualJobId;
        switch (related.getEventResourceType()) {
            case JOB:
                actualJobId = related.getId();
                break;
            case TASK:
                actualJobId = taskToJobMap.get(related.getId());
                break;
            case RUN:
                actualJobId = runToJobMap.get(related.getId());
                break;
            default:
                return false;
        }
        return jobId.equals(actualJobId);
    }

    private long getJobIdByTask(Long taskId) {
        var task = taskMapper.findTaskById(taskId);
        if (task == null) {
            return INVALID_JOB_ID;
        }
        var stepId = task.getStepId();
        var step = stepMapper.findById(stepId);
        if (step == null) {
            return INVALID_JOB_ID;
        }
        return step.getJobId();
    }

    private long getJobIdByRun(Long runId) {
        var run = runMapper.get(runId);
        if (run == null) {
            return INVALID_JOB_ID;
        }
        var taskId = run.getTaskId();
        return getJobIdByTask(taskId);
    }
}
