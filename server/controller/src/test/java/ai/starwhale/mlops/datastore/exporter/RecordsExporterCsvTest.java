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

package ai.starwhale.mlops.datastore.exporter;

import ai.starwhale.mlops.datastore.RecordList;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RecordsExporterCsvTest {

    private static final String EXPECTED = "v1,\"{\"\"mk\"\":\"\"mv\"\"}\",\"[1,2,3]\"\r\n"
            + "V1,\"{\"\"MK\"\":\"\"MV\"\"}\",\"[4,5,6]\"\r\n";

    private static final RecordList RECORD_LIST = new RecordList(
            null,
            null,
            List.of(
                    Map.of("ke1", "v1",
                            "ke2", Map.of("mk", "mv"),
                            "ke3", List.of(1, 2, 3)),
                    Map.of("ke2", Map.of("MK", "MV"),
                            "ke1", "V1",
                            "ke3", List.of(4, 5, 6))
            ),
            null,
            null);

    @Test
    public void testRecordsExporterCsv() throws IOException {
        RecordsExporterCsv exporter = new RecordsExporterCsv(new ObjectMapper());
        Assertions.assertEquals(EXPECTED, new String(exporter.asBytes(RECORD_LIST)));
    }

}
