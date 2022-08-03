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

package ai.starwhale.mlops.domain.swds;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.swds.bo.SWDSIndex;
import ai.starwhale.mlops.domain.swds.index.SWDSIndexLoaderImpl;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for {@link SWDSIndexLoaderImpl}
 */
public class SWDSIndexLoaderImplTest {

    @Test
    public void testSWDSIndexLoaderImpl() throws IOException {
        SWDSIndexLoaderImpl swdsIndexLoader = mockIndexLoader();
        String storagePath = "anyStr";
        SWDSIndex swdsIndex = swdsIndexLoader.load(storagePath);
        Assertions.assertEquals(storagePath,swdsIndex.getStoragePath());
        Assertions.assertEquals(3,swdsIndex.getSwdsBlockList().size());
    }

    public static SWDSIndexLoaderImpl mockIndexLoader() throws IOException {
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        when(storageAccessService.get(anyString())).thenReturn(new ByteArrayInputStream(INDEX_CONTENT.getBytes()));
        ObjectMapper objectMapper = new ObjectMapper();
        SWDSIndexLoaderImpl swdsIndexLoader = new SWDSIndexLoaderImpl(storageAccessService,
            objectMapper);
        return swdsIndexLoader;
    }

    public static final String INDEX_CONTENT="{\"id\": 0, \"batch\": 50, \"data\": {\"file\": \"data_ubyte_0.swds_bin\", \"offset\": 0, \"size\": 39200}, \"label\": {\"file\": \"label_ubyte_0.swds_bin\", \"offset\": 0, \"size\": 50}}1\n"
        + "{\"id\": 1, \"batch\": 50, \"data\": {\"file\": \"data_ubyte_0.swds_bin\", \"offset\": 40932, \"size\": 39200}, \"label\": {\"file\": \"label_ubyte_0.swds_bin\", \"offset\": 4068, \"size\": 50}}\n"
        + "{\"id\": 2, \"batch\": 50, \"data\": {\"file\": \"data_ubyte_0.swds_bin\", \"offset\": 81864, \"size\": 39200}, \"label\": {\"file\": \"label_ubyte_0.swds_bin\", \"offset\": 8136, \"size\": 50}}\n";
}
