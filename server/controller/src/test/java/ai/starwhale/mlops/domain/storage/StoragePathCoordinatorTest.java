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

package ai.starwhale.mlops.domain.storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class StoragePathCoordinatorTest {

    @Test
    public void testAllocatePath() {
        var sysPath = "/foo";
        StoragePathCoordinator ins = new StoragePathCoordinator(sysPath);
        assertThat(ins.allocatePluginPath("name1", "version1"),
                is("/foo/controller/plugins/panel/name1/version1"));
        assertThat(ins.allocateBundlePath(1L, "dataset", "mnist", "v1"),
                is("/foo/controller/project/1/dataset/mnist/version/v1"));
        assertThat(ins.allocateDatasetVersionPath(1L, "mnist", "v1"),
                is("/foo/controller/project/1/dataset/mnist/version/v1"));
        assertThat(ins.allocateModelPath(1L, "mnist", "v1"),
                is("/foo/controller/project/1/model/mnist/version/v1"));
        assertThat(ins.allocateRuntimePath(1L, "mnist", "v1"),
                is("/foo/controller/project/1/runtime/mnist/version/v1"));
        assertThat(ins.allocateCommonModelPoolPath(1L, "123456"),
                is("/foo/controller/project/1/common-model/123456"));
    }
}
