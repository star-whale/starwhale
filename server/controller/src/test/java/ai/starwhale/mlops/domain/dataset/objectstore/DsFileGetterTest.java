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

package ai.starwhale.mlops.domain.dataset.objectstore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DsFileGetterTest {

    @Test
    public void testDataOf() throws IOException {
        StorageAccessParser storageAccessParser = mock(StorageAccessParser.class);
        StorageAccessService storageAccessService = mock(
                StorageAccessService.class);
        when(storageAccessService.get(eq("bdc/bdcsd"), anyLong(), anyLong())).thenReturn(
                new LengthAbleInputStream(new ByteArrayInputStream("abc".getBytes()), 3));
        when(storageAccessService.head("bdcsd")).thenReturn(new StorageObjectInfo(false, 1L, null));
        when(storageAccessService.head("bdc/bdcsd")).thenReturn(new StorageObjectInfo(true, 1L, null));
        when(storageAccessParser.getStorageAccessServiceFromUri(any())).thenReturn(
                storageAccessService);
        DatasetVersionMapper versionMapper = mock(DatasetVersionMapper.class);
        when(versionMapper.find(anyLong())).thenReturn(
                DatasetVersionEntity.builder().storagePath("bdc").build());
        DsFileGetter fileGetter = new DsFileGetter(storageAccessParser, versionMapper);
        byte[] bytes = fileGetter.dataOf(1L, "bdcsd", 1L, 1L);
        Assertions.assertEquals("abc", new String(bytes));

    }

    @Test
    public void testLinkOf() throws IOException {
        StorageAccessParser storageAccessParser = mock(StorageAccessParser.class);
        StorageAccessService storageAccessService = mock(
                StorageAccessService.class);
        when(storageAccessService.head("/bdcsd")).thenReturn(new StorageObjectInfo(false, 1L, null));
        when(storageAccessService.head("bdc/bdcsd")).thenReturn(new StorageObjectInfo(true, 1L, null));
        when(storageAccessService.signedUrl(eq("bdc/bdcsd"), anyLong())).thenReturn("abc");
        when(storageAccessParser.getStorageAccessServiceFromUri(any())).thenReturn(
                storageAccessService);
        DatasetVersionMapper versionMapper = mock(DatasetVersionMapper.class);
        when(versionMapper.find(anyLong())).thenReturn(
                DatasetVersionEntity.builder().storagePath("bdc").build());
        DsFileGetter fileGetter = new DsFileGetter(storageAccessParser, versionMapper);
        Assertions.assertEquals("abc", fileGetter.linkOf(1L, "/bdcsd", 1L));
        Assertions.assertEquals("abc", fileGetter.linkOf(1L, "bdc/bdcsd", 1L));
    }

}