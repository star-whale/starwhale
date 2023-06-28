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

package ai.starwhale.mlops.domain.job;

import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CANCELLING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.CREATED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.FAIL;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.PAUSED;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.PREPARING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.RUNNING;
import static ai.starwhale.mlops.domain.task.status.TaskStatus.SUCCESS;

import ai.starwhale.mlops.domain.job.step.status.StatusRequirement;
import ai.starwhale.mlops.domain.job.step.status.StatusRequirement.RequireType;
import ai.starwhale.mlops.domain.task.status.TaskStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StatusRequirementTest {

    @Test
    public void testAny() {
        StatusRequirement trAny = new StatusRequirement(
                Set.of(FAIL), RequireType.ANY);
        Assertions.assertTrue(trAny.fit(List.of(SUCCESS, SUCCESS, SUCCESS, FAIL)));
        Assertions.assertTrue(!trAny.fit(List.of(SUCCESS, SUCCESS, RUNNING)));
        Assertions.assertTrue(trAny.fit(List.of(SUCCESS, SUCCESS, SUCCESS, SUCCESS, FAIL)));
    }

    @Test
    public void testAll() {
        StatusRequirement tr = new StatusRequirement(Set.of(SUCCESS), RequireType.ALL);
        Assertions.assertTrue(tr.fit(List.of(SUCCESS, SUCCESS, SUCCESS)));
        Assertions.assertTrue(!tr.fit(List.of(SUCCESS, SUCCESS, RUNNING)));
        Assertions.assertTrue(!tr.fit(List.of(SUCCESS, SUCCESS, SUCCESS, SUCCESS, RUNNING)));

        tr = new StatusRequirement(Set.of(SUCCESS), RequireType.ALL);
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, SUCCESS)));

        StatusRequirement tr2 = new StatusRequirement(Set.of(CREATED), RequireType.ALL);
        Assertions.assertTrue(tr2.fit(List.of(CREATED, CREATED, CREATED)));
        Assertions.assertTrue(!tr2.fit(List.of(CREATED, CREATED, CREATED, CREATED, RUNNING)));
        Assertions.assertTrue(!tr2.fit(List.of(CREATED, CREATED, CREATED, CREATED, RUNNING)));
    }

    @Test
    public void testMust() {
        StatusRequirement tr = new StatusRequirement(
                Set.of(PAUSED), RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, CANCELLING, SUCCESS, FAIL)));

        tr = new StatusRequirement(Set.of(CANCELED), RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(RUNNING, CANCELED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, CANCELED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, CANCELLING, SUCCESS, FAIL)));

        tr = new StatusRequirement(Set.of(CANCELLING), RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, SUCCESS, FAIL)));

        tr = new StatusRequirement(Set.of(SUCCESS), RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, SUCCESS)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, SUCCESS)));

        tr = new StatusRequirement(Set.of(CREATED, PREPARING, RUNNING), RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(CREATED, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(tr.fit(List.of(PREPARING, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, FAIL)));

        tr = new StatusRequirement(Set.of(PREPARING, RUNNING), RequireType.MUST);
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, RUNNING)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, PREPARING)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS)));
    }

    @Test
    public void testHaveNo() {
        StatusRequirement tr = new StatusRequirement(Set.of(FAIL), RequireType.HAVE_NO);
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, RUNNING)));

        tr = new StatusRequirement(
                Set.of(TaskStatus.PAUSED, TaskStatus.CANCELLING,
                        TaskStatus.CANCELED, TaskStatus.FAIL), RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, RUNNING)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, RUNNING)));

        tr = new StatusRequirement(Set.of(TaskStatus.values()), RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, RUNNING)));

        tr = new StatusRequirement(Set.of(TaskStatus.SUCCESS), RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, RUNNING)));

        tr = new StatusRequirement(
                Set.of(TaskStatus.FAIL, TaskStatus.CANCELLING,
                        TaskStatus.CANCELED), RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, CANCELED)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, CANCELED)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, CANCELED)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, PREPARING, SUCCESS, CREATED)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, PREPARING, SUCCESS, CREATED)));

        tr = new StatusRequirement(Set.of(TaskStatus.FAIL), RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, FAIL)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, PAUSED, CANCELLING, SUCCESS, SUCCESS)));

        tr = new StatusRequirement(
                Set.of(TaskStatus.FAIL, TaskStatus.CANCELLING,
                        TaskStatus.CREATED, TaskStatus.PAUSED,
                        TaskStatus.PREPARING, TaskStatus.RUNNING), RequireType.HAVE_NO);
        Assertions.assertTrue(tr.fit(List.of(CANCELED, SUCCESS, SUCCESS)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, SUCCESS, PREPARING)));

        tr = new StatusRequirement(Set.of(TaskStatus.CREATED), RequireType.HAVE_NO);
        Assertions.assertTrue(tr.fit(List.of(RUNNING, SUCCESS, PREPARING)));
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, SUCCESS, CREATED)));

        tr = new StatusRequirement(
                Set.of(TaskStatus.CANCELLING, TaskStatus.CANCELED,
                        TaskStatus.FAIL), RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, CANCELLING, CREATED)));
        Assertions.assertTrue(tr.fit(List.of(RUNNING, SUCCESS, CREATED)));

        tr = new StatusRequirement(Set.of(TaskStatus.values()), RequireType.HAVE_NO);
        Assertions.assertTrue(!tr.fit(List.of(RUNNING, SUCCESS, CREATED)));
    }

}


