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

package ai.starwhale.mlops.schedule.impl.docker;

import ai.starwhale.mlops.domain.task.bo.Task;
import ai.starwhale.mlops.exception.SwValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContainerTaskMapperTest {

    @Test
    public void testAll() {
        ContainerTaskMapper cm = new ContainerTaskMapper();
        Task task = Task.builder().id(1L).build();
        Assertions.assertEquals("starwhale-task-1", cm.containerNameOfTask(task));
        Assertions.assertEquals(1L, cm.taskIfOfContainer("/starwhale-task-1"));
        Assertions.assertEquals(1L, cm.taskIfOfContainer("starwhale-task-1"));
        Assertions.assertThrows(SwValidationException.class, () -> cm.taskIfOfContainer("blab-la"));

    }
}
