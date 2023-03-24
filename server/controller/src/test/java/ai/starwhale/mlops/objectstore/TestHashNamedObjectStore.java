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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TestHashNamedObjectStore {

    @Test
    public void testSave() throws IOException {
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        when(storageAccessService.head(anyString())).thenReturn(new StorageObjectInfo(false, null, null));
        HashNamedObjectStore hashNamedObjectStore = new HashNamedObjectStore(storageAccessService, "/abc");
        String blobHash = "abc123fdasd";
        String relativePath = hashNamedObjectStore.put(blobHash, mock(InputStream.class));
        Assertions.assertEquals("ab/abc123fdasd", relativePath);
        Assertions.assertEquals("/abc/ab/abc123fdasd", hashNamedObjectStore.absolutePath(blobHash));
    }

    @Test
    public void testHead() throws IOException {
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        HashNamedObjectStore hashNamedObjectStore = new HashNamedObjectStore(storageAccessService, "/abc");
        when(storageAccessService.head("/abc/h1/h121")).thenReturn(new StorageObjectInfo(false, null, null));
        when(storageAccessService.head("/abc/h2/h211")).thenReturn(new StorageObjectInfo(true, null, null));
        Assertions.assertFalse(hashNamedObjectStore.head("h121").isExists());
        Assertions.assertTrue(hashNamedObjectStore.head("h211").isExists());
    }

}
