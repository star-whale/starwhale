package ai.starwhale.mlops.api.protocol.project;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticsVO {

    private Integer modelCounts;

    private Integer datasetCounts;

    private Integer memberCounts;

    private Integer evaluationCounts;

    public static StatisticsVO empty() {
        return StatisticsVO.builder()
            .modelCounts(0)
            .datasetCounts(0)
            .memberCounts(0)
            .evaluationCounts(0)
            .build();
    }
}
