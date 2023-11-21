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

package ai.starwhale.mlops.storage;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StorageConnectionTokenTest {

    @Test
    public void testEqual() {
        StorageConnectionToken storageConnectionToken = new StorageConnectionToken("ftp",
                Map.of("a", "a1", "b", "b1", "c", "c1"));
        StorageConnectionToken storageConnectionToken2 = new StorageConnectionToken("ftp",
                Map.of("b", "b1", "a", "a1", "c", "c1"));
        Assertions.assertEquals(storageConnectionToken, storageConnectionToken2);
    }

}
