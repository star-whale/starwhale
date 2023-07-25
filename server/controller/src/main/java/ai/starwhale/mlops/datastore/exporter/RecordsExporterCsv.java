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
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

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

    public byte[] asBytes(RecordList recordList) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (final CSVPrinter printer = new CSVPrinter(stringBuilder, CSVFormat.RFC4180)) {
            recordList.getRecords().forEach(r -> {
                List<String> values = r.entrySet().stream().sorted(Entry.comparingByKey()).map(record -> {
                    if (record.getValue() instanceof String) {
                        return (String) record.getValue();
                    }
                    String v;
                    try {
                        v = objectMapper.writeValueAsString(record.getValue());
                    } catch (JsonProcessingException e) {
                        log.warn("can't jsonlize record value {} , error message is dumped", record.getValue(), e);
                        v = e.getMessage();
                    }
                    return v;
                }).collect(Collectors.toList());
                try {
                    printer.printRecord(values.toArray());
                } catch (IOException e) {
                    log.error("printing error", e);
                    throw new SwProcessException(ErrorType.SYSTEM);
                }
            });
        }
        return stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
