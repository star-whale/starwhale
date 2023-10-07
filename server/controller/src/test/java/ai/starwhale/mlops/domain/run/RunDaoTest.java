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

package ai.starwhale.mlops.domain.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.run.bo.Run;
import ai.starwhale.mlops.domain.run.mapper.RunMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunDaoTest {

    RunMapper runMapper;

    RunDao runDao;

    @BeforeEach
    public void setUp() {
        runMapper = mock(RunMapper.class);
        runDao = new RunDao(runMapper, new ObjectMapper());
    }

    @Test
    void findById() {
        when(runMapper.get(1L)).thenReturn(RunEntity.builder().id(1L).build());
        Run run = runDao.findById(1L);
        assertEquals(1L, run.getId());
    }

    @Test
    void convertEntityToBo() {
        RunEntity runEntity = RunEntity.builder().id(1L).build();
        Run run = runDao.convertEntityToBo(runEntity);
        assertEquals(1L, run.getId());
    }
}