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

package ai.starwhale.mlops.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class InvokerManagerTest {

    @Test
    public void testInvoke() {
        var manager = InvokerManager.<Integer, AtomicInteger>create()
                .addInvoker(1, param -> param.set(11))
                .addInvoker(2, param -> param.set(22))
                .addInvoker(3, param -> param.set(33));
        AtomicInteger i = new AtomicInteger();

        manager.invoke(1, i);
        assertThat(i.get(), is(11));

        manager.invoke(2, i);
        assertThat(i.get(), is(22));

        manager.invoke(3, i);
        assertThat(i.get(), is(33));

        assertThrows(UnsupportedOperationException.class,
                () -> manager.invoke(4, i));

        assertThrows(UnsupportedOperationException.class,
                () -> manager.invoke(null, i));
    }
}
