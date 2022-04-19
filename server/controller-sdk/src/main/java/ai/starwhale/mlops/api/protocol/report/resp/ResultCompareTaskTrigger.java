package ai.starwhale.mlops.api.protocol.report.resp;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * sufficient information for an Agent to run a result comparing Task
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResultCompareTaskTrigger {

    /**
     * id for the task
     */
    Long id;

    /**
     * storage directory where task result is uploaded
     */
    String resultPath;

    List<String> inferenceResultPaths;
}
