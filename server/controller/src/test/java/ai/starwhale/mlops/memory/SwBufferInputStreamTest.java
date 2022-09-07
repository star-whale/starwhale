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

package ai.starwhale.mlops.memory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ai.starwhale.mlops.memory.impl.SwByteBufferManager;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SwBufferInputStreamTest {

    private final SwByteBufferManager bufferManager = new SwByteBufferManager();
    private SwBufferInputStream inputStream;

    @BeforeEach
    public void setUp() {
        SwBuffer buffer = this.bufferManager.allocate(10);
        buffer.setString(0, "0123456789");
        this.inputStream = new SwBufferInputStream(buffer);
    }

    @Test
    public void testRead() {
        for (int i = 0; i < 10; ++i) {
            assertThat(this.inputStream.read(), is(i + (int) '0'));
        }
        assertThat(this.inputStream.read(), is(-1));
        assertThat(this.inputStream.read(), is(-1));
    }

    @Test
    public void testReadBytes() {
        var b = new byte[10];
        assertThat(this.inputStream.read(b, 1, 2), is(2));
        assertThat(Arrays.copyOfRange(b, 1, 3), is(new byte[]{'0', '1'}));
        assertThat(this.inputStream.read(b, 0, 10), is(8));
        assertThat(Arrays.copyOfRange(b, 0, 8), is(new byte[]{'2', '3', '4', '5', '6', '7', '8', '9'}));
        assertThat(this.inputStream.read(b, 0, 10), is(-1));
        assertThat(this.inputStream.read(b, 0, 10), is(-1));
    }
}
