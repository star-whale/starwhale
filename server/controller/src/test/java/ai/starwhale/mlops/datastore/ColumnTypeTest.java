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

package ai.starwhale.mlops.datastore;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ColumnTypeTest {

    @Test
    public void testDecode(){
        Object decode = ColumnType.INT32.decode(ColumnType.INT32.encode(Integer.valueOf(1)));
        Assertions.assertEquals(1,decode);

        decode = ColumnType.INT64.decode(ColumnType.INT64.encode(Long.valueOf(1)));
        Assertions.assertEquals(1l,decode);

        decode = ColumnType.FLOAT32.decode(ColumnType.FLOAT32.encode(1.003f));
        Assertions.assertEquals(1.003f,decode);

        decode = ColumnType.FLOAT64.decode(ColumnType.FLOAT64.encode(1d));
        Assertions.assertEquals(1d,decode);
    }

}
