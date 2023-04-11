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

package ai.starwhale.mlops.datastore.parquet;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SwOutputFileTest {

    @Test
    public void test() throws IOException {
        var storageAccessService = Mockito.mock(StorageAccessService.class);
        doAnswer(x -> {
            InputStream in = x.getArgument(1);
            in.readNBytes(9600);
            throw new IOException();
        }).when(storageAccessService).put(anyString(), any(InputStream.class));
        var f = new SwOutputFile(storageAccessService, "t");
        var data1 = new byte[10000];
        var out1 = f.create(data1.length);
        out1.write(data1);
        assertThrows(IOException.class, out1::close);

        var data2 = new byte[100000];
        var out2 = f.create(data2.length);
        assertThrows(IOException.class, () -> out2.write(data2));
    }

}
