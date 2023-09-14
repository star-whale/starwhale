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

import ai.starwhale.mlops.api.protocol.event.EventRequest;
import ai.starwhale.mlops.api.protocol.event.EventVo;
import ai.starwhale.mlops.domain.event.mapper.EventMapper;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwNotFoundException.ResourceType;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventService {
    // task to job cache
    // TODO use LRU cache
    private final ConcurrentHashMap<Long, Long> taskToJobMap = new ConcurrentHashMap<>();

    private final TaskMapper taskMapper;
    private final StepMapper stepMapper;
    private final EventMapper eventMapper;
    private final EventConverter eventConverter;
    private final JobDao jobDao;

    public EventService(
            TaskMapper taskMapper,
            StepMapper stepMapper,
            EventMapper eventMapper,
            EventConverter eventConverter,
            JobDao jobDao
    ) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.eventMapper = eventMapper;
        this.eventConverter = eventConverter;
        this.jobDao = jobDao;
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
        var events = eventMapper.listEvents(related.getResource(), related.getId());
        if (events == null) {
            return List.of();
        }
        return events.stream().map(eventConverter::toVo).collect(Collectors.toList());
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
            related.setResource(EventRequest.EventResource.JOB);
            related.setId(jobId);
        }
        return getEvents(related);
    }

    private void validateOwnership(Long jobId, EventRequest.RelatedResource related) {
        if (!taskBelongsToTheJob(jobId, related)) {
            throw new SwNotFoundException(ResourceType.BUNDLE, "related resource is not found");
        }
    }

    private boolean taskBelongsToTheJob(Long jobId, @Nullable EventRequest.RelatedResource related) {
        if (related == null) {
            return true;
        }

        if (related.getResource().equals(EventRequest.EventResource.JOB)) {
            return related.getId().equals(jobId);
        }

        if (related.getResource().equals(EventRequest.EventResource.TASK)) {
            var job = taskToJobMap.computeIfAbsent(related.getId(), id -> {
                var task = taskMapper.findTaskById(id);
                if (task == null) {
                    return null;
                }
                var stepId = task.getStepId();
                var step = stepMapper.findById(stepId);
                if (step == null) {
                    return null;
                }
                return step.getJobId();
            });
            return jobId.equals(job);
        }

        return false;
    }
}
