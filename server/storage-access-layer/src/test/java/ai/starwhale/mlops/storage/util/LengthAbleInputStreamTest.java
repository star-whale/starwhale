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

package ai.starwhale.mlops.storage.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LengthAbleInputStreamTest {

    private LengthAbleInputStream inputStream;

    @BeforeEach
    public void setUp() {
        this.inputStream =
                new LengthAbleInputStream(
                        new ByteArrayInputStream("test test".getBytes(StandardCharsets.UTF_8)),
                        4);
    }

    @Test
    public void testRead() throws IOException {
        assertThat(this.inputStream.read(), is((int) 't'));
        assertThat(this.inputStream.read(), is((int) 'e'));
        assertThat(this.inputStream.read(), is((int) 's'));
        assertThat(this.inputStream.read(), is((int) 't'));
        assertThat(this.inputStream.read(), is(-1));
        assertThat(this.inputStream.read(), is(-1));
    }

    @Test
    public void testRead2() throws IOException {
        byte[] b;
        b = new byte[0];
        assertThat(this.inputStream.read(b), is(0));
        assertThat(new String(b), is(""));
        b = new byte[1];
        assertThat(this.inputStream.read(b), is(1));
        assertThat(new String(b), is("t"));
        b = new byte[2];
        assertThat(this.inputStream.read(b), is(2));
        assertThat(new String(b), is("es"));
        b = new byte[3];
        assertThat(this.inputStream.read(b), is(1));
        assertThat(new String(b), is("t\0\0"));
        b = new byte[4];
        assertThat(this.inputStream.read(b), is(-1));
        assertThat(new String(b), is("\0\0\0\0"));
        b = new byte[5];
        assertThat(this.inputStream.read(b), is(-1));
        assertThat(new String(b), is("\0\0\0\0\0"));
    }

    @Test
    public void testRead3() throws IOException {
        var b = new byte[10];
        assertThat(this.inputStream.read(b, 0, 1), is(1));
        assertThat(new String(b), is("t\0\0\0\0\0\0\0\0\0"));
        assertThat(this.inputStream.read(b, 1, 2), is(2));
        assertThat(new String(b), is("tes\0\0\0\0\0\0\0"));
        assertThat(this.inputStream.read(b, 3, 3), is(1));
        assertThat(new String(b), is("test\0\0\0\0\0\0"));
        assertThat(this.inputStream.read(b, 4, 4), is(-1));
        assertThat(new String(b), is("test\0\0\0\0\0\0"));
        assertThat(this.inputStream.read(b, 4, 5), is(-1));
        assertThat(new String(b), is("test\0\0\0\0\0\0"));
    }

    @Test
    public void testReadNbytes() throws IOException {
        assertThat(new String(inputStream.readNBytes(1)), is("t"));
        assertThat(new String(inputStream.readNBytes(2)), is("es"));
        assertThat(new String(inputStream.readNBytes(3)), is("t"));
        assertThat(new String(inputStream.readNBytes(4)), is(""));
        assertThat(new String(inputStream.readNBytes(5)), is(""));
    }

    @Test
    public void testReadNbytes2() throws IOException {
        var b = new byte[10];
        assertThat(inputStream.readNBytes(b, 0, 1), is(1));
        assertThat(new String(b), is("t\0\0\0\0\0\0\0\0\0"));
        assertThat(inputStream.readNBytes(b, 1, 2), is(2));
        assertThat(new String(b), is("tes\0\0\0\0\0\0\0"));
        assertThat(inputStream.readNBytes(b, 3, 3), is(1));
        assertThat(new String(b), is("test\0\0\0\0\0\0"));
        assertThat(inputStream.readNBytes(b, 4, 4), is(0));
        assertThat(new String(b), is("test\0\0\0\0\0\0"));
        assertThat(inputStream.readNBytes(b, 4, 5), is(0));
        assertThat(new String(b), is("test\0\0\0\0\0\0"));
    }

    @Test
    public void testReadAllBytes() throws IOException {
        assertThat(new String(inputStream.readAllBytes()), is("test"));
        assertThat(new String(inputStream.readAllBytes()), is(""));
        assertThat(new String(inputStream.readAllBytes()), is(""));
    }

    @Test
    public void testSkip() throws IOException {
        assertThat(inputStream.skip(0), is(0L));
        assertThat(inputStream.skip(1), is(1L));
        assertThat(inputStream.skip(2), is(2L));
        assertThat(inputStream.skip(3), is(1L));
        assertThat(inputStream.skip(4), is(0L));
        assertThat(inputStream.skip(5), is(0L));
    }
}

