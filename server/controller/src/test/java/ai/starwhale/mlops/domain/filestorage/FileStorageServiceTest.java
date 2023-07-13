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

package ai.starwhale.mlops.domain.filestorage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;


public class FileStorageServiceTest {
    private final StorageAccessService storageAccessService = mock(StorageAccessService.class);
    private FileStorageService fileStorageService;

    private static final String rootPath = "mock-files/";
    private static final String flag = "my-dir";
    private static final String uuidStr = "123e4567-e89b-12d3-a456-426655440000";

    private static final String pathPrefix = rootPath + flag + "/" + uuidStr + "/";

    @BeforeEach
    public void init() {
        fileStorageService = new FileStorageService(
                storageAccessService, rootPath, "4h");
    }

    @Test
    public void testApplyPath() {
        var expect = UUID.fromString(uuidStr);
        try (MockedStatic<UUID> uuidMocked = mockStatic(UUID.class)) {
            uuidMocked.when(UUID::randomUUID).thenReturn(expect);
            assertEquals(pathPrefix, fileStorageService.generatePathPrefix(flag));
        }
    }

    @Test
    public void testGeneratePutUrl() throws IOException {
        given(storageAccessService.signedPutUrl(any(), any(), any())).willReturn("signedUrl");
        assertThrows(SwValidationException.class, () ->
                fileStorageService.generateSignedPutUrls("invalidPath", Set.of("a.txt", "b.txt", "c.txt")));

        assertThat("signed put urls",
                fileStorageService.generateSignedPutUrls(pathPrefix, Set.of("a.txt", "b.txt")),
                is(Map.of(
                    "a.txt", "signedUrl",
                    "b.txt", "signedUrl"
                ))
        );

    }

    @Test
    public void testGenerateGetUrl() throws IOException {
        given(storageAccessService.list(pathPrefix))
                .willReturn(List.of(pathPrefix + "a.txt", pathPrefix + "b.txt").stream());
        given(storageAccessService.signedUrl(any(), any())).willReturn("signedGetUrl");
        assertThrows(SwValidationException.class, () ->
                fileStorageService.generateSignedGetUrls("invalidPath"));

        assertThat("signed get urls", fileStorageService.generateSignedGetUrls(pathPrefix),
                is(Map.of(
                    "a.txt", "signedGetUrl",
                    "b.txt", "signedGetUrl"
                ))
        );
    }

    @Test
    public void testDeleteFile() throws IOException {
        assertThrows(SwValidationException.class, () ->
                fileStorageService.deleteFiles("invalidPath", Set.of()));

        fileStorageService.deleteFiles(pathPrefix, Set.of("a.txt", "b.txt"));
        verify(storageAccessService, times(2)).delete(any());
    }

}
