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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Component
@Slf4j
public class RecordsStreamingExporterCsv implements RecordsStreamingExporter {

    final ObjectMapper objectMapper;

    public RecordsStreamingExporterCsv(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public int exportTo(RecordList recordList, OutputStream outputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        if (CollectionUtils.isEmpty(recordList.getRecords())) {
            return 0;
        }
        try (final CSVPrinter printer = new CSVPrinter(stringBuilder, CSVFormat.RFC4180)) {
            List<String> headers = recordList.getRecords().get(0).entrySet().stream().sorted(Entry.comparingByKey())
                    .map(Entry::getKey).collect(Collectors.toList());
            printer.printRecord(headers);
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
        byte[] bytes = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes);
        return bytes.length;
    }

    @Override
    public String getFileSuffix() {
        return "csv";
    }

    @Override
    public String getWebMediaType() {
        return "text/csv;charset=utf-8";
    }
}
