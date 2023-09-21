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

package ai.starwhale.mlops.domain.upgrade.rollup;

import ai.starwhale.mlops.domain.upgrade.rollup.RollingUpdateStatusListener.ServerInstanceStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * a test for RollingUpdateStatusListeners
 */
public class RollingUpdateStatusListenersTest {

    List<Class<? extends TestListener>> expectedOrder = List.of(
            TestListener0.class,
            TestListener1.class,
            TestListener.class
    );

    @Test
    public void testNewInstanceStatusOrder() throws Throwable {
        ArrayList<Class> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            list.clear();
            RollingUpdateStatusListeners rollingUpdateStatusListeners = new RollingUpdateStatusListeners(
                    randomList(new TestListener1(list), new TestListener0(list), new TestListener(list))
            );
            rollingUpdateStatusListeners.onNewInstanceStatus(ServerInstanceStatus.BORN);
            Assertions.assertIterableEquals(expectedOrder, list);
        }

    }


    @Test
    public void testOldInstanceStatusOrder() throws Throwable {
        ArrayList<Class> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            list.clear();
            RollingUpdateStatusListeners rollingUpdateStatusListeners = new RollingUpdateStatusListeners(
                    randomList(new TestListener1(list), new TestListener0(list), new TestListener(list))
            );
            rollingUpdateStatusListeners.onOldInstanceStatus(ServerInstanceStatus.DOWN);
            Assertions.assertIterableEquals(expectedOrder, list);
        }
    }

    @NotNull
    private static List<RollingUpdateStatusListener> randomList(RollingUpdateStatusListener... objects) {
        List<RollingUpdateStatusListener> rollingUpdateStatusListeners = Arrays.asList(objects);
        Collections.shuffle(rollingUpdateStatusListeners);
        for (var l : rollingUpdateStatusListeners) {
            System.out.print(l.getClass().getSimpleName());
            System.out.print(",");
        }
        System.out.println("");
        return rollingUpdateStatusListeners;
    }

    private static class TestListener implements RollingUpdateStatusListener {

        private final ArrayList<Class> list;

        private TestListener(ArrayList<Class> list) {
            this.list = list;
        }

        @Override
        public void onNewInstanceStatus(ServerInstanceStatus status) {
            list.add(this.getClass());
        }

        @Override
        public void onOldInstanceStatus(ServerInstanceStatus status) {
            list.add(this.getClass());
        }

    }

    private static class TestListener0 extends TestListener implements OrderedRollingUpdateStatusListener {

        private TestListener0(ArrayList<Class> list) {
            super(list);
        }

        @Override
        public int getOrder() {
            return 0;
        }

    }

    private static class TestListener1 extends TestListener0 {

        private TestListener1(ArrayList<Class> list) {
            super(list);
        }

        @Override
        public int getOrder() {
            return 1;
        }

    }


}
