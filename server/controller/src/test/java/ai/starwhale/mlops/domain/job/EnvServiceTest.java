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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;

import ai.starwhale.mlops.schedule.impl.k8s.ResourceOverwriteSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EnvServiceTest {

    private EnvService service;

    @BeforeEach
    public void setUp() {
        service = new EnvService();
    }

    @Test
    public void testListDevices() {
        var res = service.listDevices();
        assertThat(res, allOf(
                notNullValue(),
                is(iterableWithSize(ResourceOverwriteSpec.SUPPORTED_DEVICES.size()))
        ));
    }
}
