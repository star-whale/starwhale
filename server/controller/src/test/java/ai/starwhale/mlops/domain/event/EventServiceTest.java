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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.event.Event.EventResource;
import ai.starwhale.mlops.api.protocol.event.Event.EventSource;
import ai.starwhale.mlops.api.protocol.event.Event.EventType;
import ai.starwhale.mlops.api.protocol.event.EventRequest;
import ai.starwhale.mlops.api.protocol.event.EventRequest.RelatedResource;
import ai.starwhale.mlops.domain.event.mapper.EventMapper;
import ai.starwhale.mlops.domain.event.po.EventEntity;
import ai.starwhale.mlops.domain.job.JobDao;
import ai.starwhale.mlops.domain.job.step.mapper.StepMapper;
import ai.starwhale.mlops.domain.job.step.po.StepEntity;
import ai.starwhale.mlops.domain.run.RunEntity;
import ai.starwhale.mlops.domain.run.mapper.RunMapper;
import ai.starwhale.mlops.domain.task.mapper.TaskMapper;
import ai.starwhale.mlops.domain.task.po.TaskEntity;
import ai.starwhale.mlops.exception.SwNotFoundException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventServiceTest {
    private final EventMapper eventMapper = mock(EventMapper.class);
    private final EventConverter eventConverter = new EventConverter();
    private final JobDao jobDao = mock(JobDao.class);
    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final StepMapper stepMapper = mock(StepMapper.class);
    private final RunMapper runMapper = mock(RunMapper.class);
    private final EventService eventService =
            new EventService(taskMapper, stepMapper, runMapper, eventMapper, eventConverter, jobDao, 100, 100);


    @Test
    void testAddEvent() {
        var req = new EventRequest();
        req.setRelatedResource(new EventRequest.RelatedResource(EventResource.JOB, 2L));
        eventService.addEvent(req);
        verify(eventMapper).insert(any());
    }

    @Test
    void testGetEvents() {
        var req = new EventRequest();
        req.setRelatedResource(new EventRequest.RelatedResource(EventResource.JOB, 2L));
        var resp = eventService.getEvents(req.getRelatedResource());
        verify(eventMapper).listEvents(EventResource.JOB, 2L);
        assertEquals(resp, List.of());

        when(eventMapper.listEvents(EventResource.JOB, 2L)).thenReturn(
                List.of(EventEntity.builder()
                        .id(1L)
                        .type(EventType.INFO)
                        .source(EventSource.CLIENT)
                        .resource(EventResource.JOB)
                        .resourceId(2L)
                        .message("message")
                        .data("data")
                        .createdTime(new Date(123L))
                        .build()));
        resp = eventService.getEvents(req.getRelatedResource());
        assertEquals(resp.size(), 1);
        var item = resp.get(0);
        assertEquals(item.getId(), 1L);
        assertEquals(item.getEventType(), EventType.INFO);
        assertEquals(item.getSource(), EventSource.CLIENT);
        assertEquals(item.getMessage(), "message");
        assertEquals(item.getData(), "data");
        assertEquals(item.getTimestamp(), 123L);
    }

    @Test
    void testAddEventForJob() {
        var req = new EventRequest();
        req.setRelatedResource(new EventRequest.RelatedResource(EventResource.JOB, 2L));
        when(jobDao.getJobId("1")).thenReturn(1L);
        // the job id is not equal to the related resource id
        assertThrows(SwNotFoundException.class, () -> eventService.addEventForJob("1", req));
        when(jobDao.getJobId("2")).thenReturn(2L);
        // the job id is equal to the related resource id
        eventService.addEventForJob("2", req);
        verify(eventMapper).insert(any());

        // the related resource is task of job
        req.setRelatedResource(new EventRequest.RelatedResource(EventResource.TASK, 3L));
        when(taskMapper.findTaskById(3L)).thenReturn(TaskEntity.builder().stepId(4L).build());
        when(stepMapper.findById(4L)).thenReturn(StepEntity.builder().jobId(5L).build());
        when(jobDao.getJobId("5")).thenReturn(5L);
        reset(eventMapper);
        eventService.addEventForJob("5", req);
        verify(eventMapper).insert(any());

        // the related resource is run of task
        req.setRelatedResource(new EventRequest.RelatedResource(EventResource.RUN, 71L));
        when(runMapper.get(71L)).thenReturn(RunEntity.builder().id(71L).taskId(3L).build());
        reset(eventMapper);
        eventService.addEventForJob("5", req);
        verify(eventMapper).insert(any());

        reset(eventMapper);
        // the related resource is task, but the task is not found
        when(taskMapper.findTaskById(3L)).thenReturn(null);
        // no exception is thrown, because the task to job mapping is cached
        eventService.addEventForJob("5", req);
        verify(eventMapper).insert(any());

        reset(eventMapper);
        req.getRelatedResource().setId(7L);
        when(taskMapper.findTaskById(7L)).thenReturn(null);
        assertThrows(SwNotFoundException.class, () -> eventService.addEventForJob("5", req));
        verify(eventMapper, never()).insert(any());

        // the related resource is task, but the step is not found
        when(taskMapper.findTaskById(7L)).thenReturn(TaskEntity.builder().stepId(4L).build());
        when(stepMapper.findById(4L)).thenReturn(null);
        assertThrows(SwNotFoundException.class, () -> eventService.addEventForJob("5", req));
        verify(eventMapper, never()).insert(any());

        // the related resource is task, but the job is not the given job
        when(taskMapper.findTaskById(7L)).thenReturn(TaskEntity.builder().stepId(4L).build());
        when(stepMapper.findById(4L)).thenReturn(StepEntity.builder().jobId(8L).build());
        when(jobDao.getJobId("9")).thenReturn(9L);
        assertThrows(SwNotFoundException.class, () -> eventService.addEventForJob("9", req));
        verify(eventMapper, never()).insert(any());

        // the related resource is task, but the job is not found
        when(taskMapper.findTaskById(10L)).thenReturn(TaskEntity.builder().stepId(4L).build());
        when(stepMapper.findById(4L)).thenReturn(StepEntity.builder().jobId(null).build());
        assertThrows(SwNotFoundException.class, () -> eventService.addEventForJob("9", req));
        verify(eventMapper, never()).insert(any());
    }

    @Test
    void testGetEventForJob() {
        var related = new EventRequest.RelatedResource(EventResource.JOB, 2L);
        when(jobDao.getJobId("1")).thenReturn(1L);
        assertThrows(SwNotFoundException.class,
                () -> eventService.getEventsForJob("1", new RelatedResource(EventResource.JOB, 2L)));

        // use job if related is null
        eventService.getEventsForJob("1", null);
        verify(eventMapper).listEvents(EventResource.JOB, 1L);

        // normal case
        when(jobDao.getJobId("2")).thenReturn(2L);
        eventService.getEventsForJob("2", related);
        verify(eventMapper).listEvents(EventResource.JOB, 2L);

        // the related resource is task of job
        when(taskMapper.findTaskById(3L)).thenReturn(TaskEntity.builder().stepId(4L).build());
        when(stepMapper.findById(4L)).thenReturn(StepEntity.builder().jobId(5L).build());
        when(jobDao.getJobId("5")).thenReturn(5L);
        reset(eventMapper);
        related = new EventRequest.RelatedResource(EventResource.TASK, 3L);
        eventService.getEventsForJob("5", related);
        verify(eventMapper).listEvents(EventResource.TASK, 3L);

        reset(eventMapper);
        // the related resource is task, but the task is not found
        when(taskMapper.findTaskById(4L)).thenReturn(null);
        assertThrows(SwNotFoundException.class,
                () -> eventService.getEventsForJob("5", new RelatedResource(EventResource.TASK, 4L)));

        // the related resource is run of task
        when(runMapper.get(71L)).thenReturn(RunEntity.builder().id(71L).taskId(3L).build());
        reset(eventMapper);
        eventService.getEventsForJob("5", new RelatedResource(EventResource.RUN, 71L));
        verify(eventMapper).listEvents(EventResource.RUN, 71L);
    }
}
