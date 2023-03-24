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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.dataset.objectstore.StorageAccessParser;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UriAccessorTest {

    @Test
    public void testDataOf() throws IOException {
        StorageAccessParser storageAccessParser = mock(StorageAccessParser.class);
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        when(storageAccessService.get(eq("uri/abc"), anyLong(),
                anyLong())).thenReturn(new LengthAbleInputStream(new ByteArrayInputStream("abc".getBytes()), 3));
        when(storageAccessParser.getStorageAccessServiceFromUri(any())).thenReturn(
                storageAccessService);
        UriAccessor fileGetter = new UriAccessor(storageAccessParser);
        byte[] bytes = fileGetter.dataOf(1L, "bdcsd", "uri/abc", 1L, 1L);
        Assertions.assertEquals("abc", new String(bytes));

    }

    @Test
    public void testDataOfHttp() {
        UriAccessor fileGetter = new UriAccessor(null);
        byte[] bytes = fileGetter.dataOf(1L, "ds",
                "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/celeba/img_align_celeba/000003.jpg",
                -1L, -1L);
        Assertions.assertEquals(4253, bytes.length);

        Assertions.assertThrows(SwProcessException.class, () -> fileGetter.dataOf(1L, "ds",
                "https://starwhale-examples.oss-cn-beijing.aliyuncs.com/dataset/celeba/img_align_celeba/000003.jpgasfa",
                -1L, -1L));

        Assertions.assertThrows(SwValidationException.class, () -> fileGetter.dataOf(1L, "ds",
                "https://abcd:adg",
                -1L, -1L));
    }

    @Test
    public void testLinkOf() throws IOException {
        StorageAccessParser storageAccessParser = mock(StorageAccessParser.class);
        StorageAccessService storageAccessService = mock(
                StorageAccessService.class);
        when(storageAccessService.signedUrl(eq("/bdc/bdcsd"), anyLong())).thenReturn("abc");
        when(storageAccessService.signedUrl(eq("/uri/1"),
                anyLong())).thenReturn("ABC");
        when(storageAccessParser.getStorageAccessServiceFromUri(any())).thenReturn(
                storageAccessService);
        UriAccessor fileGetter = new UriAccessor(storageAccessParser);
        Assertions.assertEquals("ABC", fileGetter.linkOf(1L, "bdc", "/uri/1", 1L));
        Assertions.assertEquals("abc", fileGetter.linkOf(1L, "ds", "s3://host:9080/bucket/bdc/bdcsd", 1L));
    }

}