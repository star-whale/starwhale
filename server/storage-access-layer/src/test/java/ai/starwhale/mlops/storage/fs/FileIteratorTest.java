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
package ai.starwhale.mlops.storage.fs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FileIteratorTest {
    @TempDir
    private File rootDir;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @BeforeEach
    public void setUp() throws IOException {
        var a = new File(this.rootDir, "a");
        a.mkdir();
        new File(a, "b").createNewFile();
        new File(a, "b1").createNewFile();
        new File(a, "b2").createNewFile();

        var a1 = new File(this.rootDir, "a1");
        a1.mkdir();
        new File(a1, "b1").mkdir();
        new File(a1, "b2").createNewFile();

        var a1_b = new File(a1, "b");
        a1_b.mkdir();
        new File(a1_b, "c").createNewFile();
        new File(a1_b, "c1").createNewFile();

        var a2 = new File(this.rootDir, "a2");
        a2.mkdir();
        new File(new File(a2, "b"), "c").mkdirs();
        new File(a2, "b1").mkdir();

        new File(this.rootDir, "a10").createNewFile();
        new File(this.rootDir, "a11").createNewFile();
        new File(this.rootDir, "a.txt").createNewFile();
    }

    private List<String> getAll(FileIterator fileIterator) {
        var ret = new ArrayList<String>();
        while (fileIterator.hasNext()) {
            ret.add(fileIterator.next());
        }
        return ret;
    }

    @Test
    public void testScan() {
        assertThat(getAll(new FileIterator(this.rootDir, "a")),
                is(List.of("a.txt", "a/b", "a/b1", "a/b2", "a1/b/c", "a1/b/c1", "a1/b2", "a10", "a11")));
        assertThat(getAll(new FileIterator(this.rootDir, "a/")),
                is(List.of("a/b", "a/b1", "a/b2")));
        assertThat(getAll(new FileIterator(this.rootDir, "a/b")),
                is(List.of("a/b", "a/b1", "a/b2")));
        assertThat(getAll(new FileIterator(this.rootDir, "a1/b/")),
                is(List.of("a1/b/c", "a1/b/c1")));
        assertThat(getAll(new FileIterator(this.rootDir, "a1/b/c1")),
                is(List.of("a1/b/c1")));
        assertThat(getAll(new FileIterator(this.rootDir, "a1/b/c1/")),
                is(List.of()));
        assertThat(getAll(new FileIterator(this.rootDir, "b")),
                is(List.of()));
    }
}
