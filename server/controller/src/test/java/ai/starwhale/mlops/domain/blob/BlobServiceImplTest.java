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

package ai.starwhale.mlops.domain.blob;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlobServiceImplTest {

    private StorageAccessService storageAccessService;

    private BlobService blobService;

    @BeforeEach
    public void setUp() {
        this.storageAccessService = new StorageAccessServiceMemory();
        this.blobService = new BlobServiceImpl(this.storageAccessService, "blob/", "4h");
    }

    @SneakyThrows
    @Test
    public void testGenerateBlobId() {
        int n = 100000;
        var idSet = new HashSet<String>();
        for (int i = 0; i < n; ++i) {
            var id = this.blobService.generateBlobId();
            assertThat(Long.parseUnsignedLong(id, 16), not(is(0L)));
            idSet.add(id);
        }
        assertThat(idSet.size(), is(n));
    }

    @SneakyThrows
    @Test
    public void testReadBlobRef() {
        this.storageAccessService.put("blob/ref/s-5", "1".getBytes(StandardCharsets.UTF_8));
        assertThat(this.blobService.readBlobRef("S", 5), is("1"));
        assertThrows(FileNotFoundException.class, () -> this.blobService.readBlobRef("S", 6));
    }

    @SneakyThrows
    @Test
    public void testGenerateBlobRef() {
        this.storageAccessService.put("blob/1", "11".getBytes(StandardCharsets.UTF_8));
        assertThat(this.blobService.generateBlobRef("1"), is("1"));
        assertThat(this.blobService.readBlobRef(DigestUtils.md5Hex("11"), 2), is("1"));

        this.storageAccessService.put("blob/2", "22".getBytes(StandardCharsets.UTF_8));
        this.storageAccessService.put("blob/ref/" + DigestUtils.md5Hex("22").toLowerCase() + "-2",
                "3".getBytes(StandardCharsets.UTF_8));
        assertThat(this.blobService.generateBlobRef("2"), is("3"));

        assertThrows(SwValidationException.class, () -> this.blobService.generateBlobRef("3"));
    }

    @SneakyThrows
    @Test
    public void testReadBlob() {
        this.storageAccessService.put("blob/1", "1234".getBytes(StandardCharsets.UTF_8));
        try (var data = this.blobService.readBlob("1")) {
            assertThat(data.readAllBytes(), is("1234".getBytes(StandardCharsets.UTF_8)));
        }
        assertThat(this.blobService.readBlobAsByteArray("1"), is("1234".getBytes(StandardCharsets.UTF_8)));
        try (var data = this.blobService.readBlob("1", 2, 2)) {
            assertThat(data.readAllBytes(), is("34".getBytes(StandardCharsets.UTF_8)));
        }
        assertThrows(FileNotFoundException.class, () -> this.blobService.readBlob("2").close());
    }

    @SneakyThrows
    @Test
    public void testGetSignedUrl() {
        assertThat(this.blobService.getSignedUrl("1"), is("blob/1"));
        assertThat(this.blobService.getSignedPutUrl("1"), is("blob/1"));
    }
}
