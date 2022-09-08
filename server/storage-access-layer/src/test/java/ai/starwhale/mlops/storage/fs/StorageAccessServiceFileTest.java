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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class StorageAccessServiceFileTest {

    @TempDir
    private File rootDir;

    private StorageAccessServiceFile service;

    @BeforeEach
    public void setUp() {
        this.service = new StorageAccessServiceFile(this.rootDir.getAbsolutePath());
    }

    @Test
    public void testAll() throws IOException {
        this.service.put("t1", "c:t1".getBytes(StandardCharsets.UTF_8));
        this.service.put("t2", "c:t2".getBytes(StandardCharsets.UTF_8));
        this.service.put("t/t3", "c:t/t3".getBytes(StandardCharsets.UTF_8));
        this.service.put("d/a", "c:d/a".getBytes(StandardCharsets.UTF_8));
        assertThat(this.service.list("t").collect(Collectors.toList()), is(List.of("t/t3", "t1", "t2")));
        try (var in = this.service.get("t1")) {
            assertThat(in.readAllBytes(), is("c:t1".getBytes(StandardCharsets.UTF_8)));
        }
        try (var in = this.service.get("t/t3")) {
            assertThat(in.readAllBytes(), is("c:t/t3".getBytes(StandardCharsets.UTF_8)));
        }
        this.service.delete("t/t3");
        assertThat(new File(this.rootDir, "t").exists(), is(false));
        this.service.delete("t1");
        this.service.delete("t2");
        this.service.delete("d/a");
        assertThat(this.rootDir.list(), is(new String[0]));
        assertThat(this.rootDir.exists(), is(true));
    }

}
