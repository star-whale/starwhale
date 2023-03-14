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

import ai.starwhale.mlops.datastore.type.BaseValue;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.apache.parquet.hadoop.ParquetWriter;

public class SwWriter {
    public static void writeWithBuilder(
            SwParquetWriterBuilder builder, Iterator<Map<String, BaseValue>> iterator) throws IOException {
        ParquetWriter<Map<String, BaseValue>> writer = null;
        try {
            writer = builder.build();
            while (iterator.hasNext()) {
                writer.write(iterator.next());
            }
            builder.success();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

}
