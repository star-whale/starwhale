package ai.starwhale.mlops.resulting.impl.clsmulti.ppl;

import ai.starwhale.mlops.resulting.impl.clsmulti.InferenceResult;
import ai.starwhale.mlops.resulting.impl.clsmulti.metrics.MCIndicator;
import ai.starwhale.mlops.resulting.pipline.DataResultCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * calculate data level indicators for multiple classification problem
 */
@Component
@Slf4j
public class McDataResultCalculator implements DataResultCalculator<MCIndicator> {

    final ObjectMapper objectMapper;

    public McDataResultCalculator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MCIndicator> calculate(InputStream inferenceResultIS) throws IOException{
        final Queue<String> jsonLines = readLinesFromIS(inferenceResultIS);
        return jsonLines.parallelStream().map(jsonline -> {
                InferenceResult inferenceResult;
                try {
                    inferenceResult = objectMapper
                        .readValue(jsonline, InferenceResult.class);
                } catch (JsonProcessingException e) {
                    log.error("unknown format of json line {}", jsonline);
                    return null;
                }
                return inferenceResult;
            }).filter(Objects::nonNull)
            .map(InferenceResult::toMCIndicators)
            .flatMap(Collection::parallelStream)
            .collect(Collectors.toList());
    }

    Queue<String> readLinesFromIS(InputStream is) throws IOException {
        Queue<String> lines = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while (reader.ready()) {
            lines.offer(reader.readLine());
        }
        return lines;
    }
}
