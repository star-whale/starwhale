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

package ai.starwhale.mlops.domain.upgrade.rollup.starter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.upgrade.mapper.StarterMapper;
import ai.starwhale.mlops.domain.upgrade.mapper.TableCheckMapper;
import ai.starwhale.mlops.domain.upgrade.po.StarterEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StarterTest {

    TableCheckMapper tableCheckMapper;
    StarterMapper starterMapper;
    Starter starter;

    @BeforeEach
    public void setup() {
        tableCheckMapper = mock(TableCheckMapper.class);
        starterMapper = mock(StarterMapper.class);
        starter = new Starter(tableCheckMapper, starterMapper);
    }

    @Test
    public void testRollupStart() {
        when(tableCheckMapper.checkTable("starter")).thenReturn(1);
        when(starterMapper.getStarter()).thenReturn(StarterEntity.Starter.ROLLUP);
        boolean result = starter.rollupStart();
        assertTrue(result);
        starter.reset();
        verify(starterMapper).resetStarter();
    }

    @Test
    public void testNoTable() {
        when(tableCheckMapper.checkTable("starter")).thenReturn(0);
        assertFalse(starter.rollupStart());
        starter.reset();
        verify(starterMapper, times(0)).resetStarter();
    }

    @Test
    public void testHasTableSingleton() {
        when(tableCheckMapper.checkTable("starter")).thenReturn(1);
        when(starterMapper.getStarter()).thenReturn(StarterEntity.Starter.SINGLETON);
        assertFalse(starter.rollupStart());
        starter.reset();
        verify(starterMapper).resetStarter();
    }
}