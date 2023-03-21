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

package ai.starwhale.mlops.objectstore;

import static org.mockito.Mockito.mock;

import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TestHashNamedObjectStore {

    @Test
    public void testSave() throws IOException {
        StorageAccessService mock = mock(StorageAccessService.class);
        HashNamedObjectStore hashNamedObjectStore = new HashNamedObjectStore(mock, "/abc");
        String relativePath = hashNamedObjectStore.put("abc123fdasd", mock(InputStream.class));
        Assertions.assertEquals("ab/abc123fdasd", relativePath);
        Assertions.assertEquals("/abc/ab/abc123fdasd", hashNamedObjectStore.absolutePath(relativePath));
    }

}
