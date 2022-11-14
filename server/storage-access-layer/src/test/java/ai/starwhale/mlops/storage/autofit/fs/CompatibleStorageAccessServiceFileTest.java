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

package ai.starwhale.mlops.storage.autofit.fs;

import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageUri;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CompatibleStorageAccessServiceFileTest {

    @Test
    public void testCompatible() throws URISyntaxException {
        CompatibleStorageAccessServiceFile compatibleStorageAccessServiceFile = new CompatibleStorageAccessServiceFile(
                mock(
                        StorageAccessService.class), "/tmp/dir");
        Assertions.assertTrue(compatibleStorageAccessServiceFile.compatibleWith(new StorageUri("file:///tmp/dir/x")));
    }

    @Test
    public void testNotCompatible() throws URISyntaxException {
        CompatibleStorageAccessServiceFile compatibleStorageAccessServiceFile = new CompatibleStorageAccessServiceFile(
                mock(
                        StorageAccessService.class), "/tmp/dir");
        Assertions.assertFalse(compatibleStorageAccessServiceFile.compatibleWith(new StorageUri("file:///tmp/dir1/x")));
    }

}

