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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * export as <a href="https://www.rfc-editor.org/rfc/rfc4180">csv format</a>
 */
@Component
@Slf4j
public class RecordsExporterCsv implements RecordsExporter {

    final ObjectMapper objectMapper;

    public RecordsExporterCsv(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] asBytes(RecordList recordList) {
        StringBuilder stringBuilder = new StringBuilder();
        recordList.getRecords().forEach(r -> {
            r.entrySet().stream().sorted(Entry.comparingByKey()).forEach(record -> {
                String v;
                try {
                    v = objectMapper.writeValueAsString(record.getValue());
                } catch (JsonProcessingException e) {
                    log.warn("can't jsonlize record value {} , error message is dumped", record.getValue(), e);
                    v = e.getMessage();
                }
                stringBuilder.append(wrap(v));
                stringBuilder.append(",");
            });
            stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), "\n");
        });
        return stringBuilder.toString().getBytes();
    }

    private static final Pattern QUOTE = Pattern.compile("^\".*\"$");

    /**
     * https://www.rfc-editor.org/rfc/rfc4180
     *
     * @param v csv value
     * @return conform to rfc4180 value
     */
    private String wrap(String v) {
        if (QUOTE.matcher(v).matches()) {
            return v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";

    }
}
