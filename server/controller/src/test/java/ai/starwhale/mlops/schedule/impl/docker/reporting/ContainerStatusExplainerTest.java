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

package ai.starwhale.mlops.schedule.impl.docker.reporting;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.run.bo.RunStatus;
import com.github.dockerjava.api.model.Container;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContainerStatusExplainerTest {


    @Test
    public void testStatus() {
        ContainerStatusExplainer containerStatusExplainer = new ContainerStatusExplainer();
        Container c = mock(Container.class);
        when(c.getState()).thenReturn("running");
        Assertions.assertEquals(RunStatus.RUNNING, containerStatusExplainer.statusOf(c));

        when(c.getState()).thenReturn("exited");
        when(c.getStatus()).thenReturn("Exited (0) blab-la");
        Assertions.assertEquals(RunStatus.FINISHED, containerStatusExplainer.statusOf(c));

        when(c.getState()).thenReturn("exited");
        when(c.getStatus()).thenReturn("Exited (1) blab-la");
        Assertions.assertEquals(RunStatus.FAILED, containerStatusExplainer.statusOf(c));
    }


}
